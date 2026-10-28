package br.com.desafio.gestaopessoas;

import br.com.desafio.gestaopessoas.infrastructure.messaging.*;
import br.com.desafio.gestaopessoas.infrastructure.persistence.OutboxEventJpaRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "app.messaging.enabled=true", "app.outbox.scheduling-enabled=false",
        "spring.kafka.listener.auto-startup=false", "spring.kafka.admin.auto-create=false"
})
class OutboxPublishingIntegrationTest {
    @Autowired OutboxPublishingTransaction publisher;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean OutboxEventJpaRepository repository;
    @MockitoBean KafkaOutboxDelivery delivery;
    private final UUID eventId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    @BeforeEach void setup() {
        var now = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        jdbc.update("""
                insert into outbox_events(event_id,employee_id,event_type,schema_version,occurred_at,payload,available_at)
                values(?,?,?,1,?,?,?)
                """, eventId, employeeId, "EmployeeCreatedEvent", now, "{}", now);
    }

    @AfterEach void cleanup() { jdbc.update("delete from outbox_events where event_id=?", eventId); }

    @Test void failureKeepsEventPendingAndSuccessfulRetryMarksSameEvent() throws Exception {
        doThrow(new TimeoutException("broker unavailable")).doNothing().when(delivery).send(employeeId, "{}");
        assertFalse(publisher.publish(eventId));
        assertNull(jdbc.queryForObject("select published_at from outbox_events where event_id=?", OffsetDateTime.class, eventId));
        assertEquals(1, jdbc.queryForObject("select attempts from outbox_events where event_id=?", Integer.class, eventId));
        assertFalse(publisher.publish(eventId), "Deve respeitar o intervalo entre tentativas");
        verify(delivery, times(1)).send(employeeId, "{}");
        jdbc.update("update outbox_events set available_at=? where event_id=?", OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1), eventId);
        assertTrue(publisher.publish(eventId));
        assertNotNull(jdbc.queryForObject("select published_at from outbox_events where event_id=?", OffsetDateTime.class, eventId));
        assertEquals(2, jdbc.queryForObject("select attempts from outbox_events where event_id=?", Integer.class, eventId));
        assertFalse(publisher.publish(eventId), "Evento publicado não deve ser reenviado pelo polling normal");
        verify(delivery, times(2)).send(employeeId, "{}");
    }

    @Test void restartAfterSendBeforeDatabaseCommitCanResendSameEvent() throws Exception {
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("database unavailable"))
                .when(repository).flush();
        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class, () -> publisher.publish(eventId));
        assertNull(jdbc.queryForObject("select published_at from outbox_events where event_id=?", OffsetDateTime.class, eventId));
        reset(repository);
        assertTrue(publisher.publish(eventId));
        verify(delivery, times(2)).send(employeeId, "{}");
        assertEquals(eventId, jdbc.queryForObject("select event_id from outbox_events where event_id=?", UUID.class, eventId));
    }

    @Test void pessimisticLockSerializesAttemptsForTheSameEvent() throws Exception {
        var sending = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        doAnswer(invocation -> {
            sending.countDown();
            if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new TimeoutException("test coordination");
            return null;
        }).when(delivery).send(employeeId, "{}");
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> publisher.publish(eventId));
            try {
                assertTrue(sending.await(5, java.util.concurrent.TimeUnit.SECONDS));
                var second = executor.submit(() -> publisher.publish(eventId));
                assertThrows(TimeoutException.class, () -> second.get(200, java.util.concurrent.TimeUnit.MILLISECONDS));
                release.countDown();
                assertTrue(first.get(5, java.util.concurrent.TimeUnit.SECONDS));
                assertFalse(second.get(5, java.util.concurrent.TimeUnit.SECONDS));
                verify(delivery, times(1)).send(employeeId, "{}");
            } finally {
                release.countDown();
            }
        }
    }
}
