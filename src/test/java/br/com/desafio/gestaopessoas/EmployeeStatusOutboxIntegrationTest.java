package br.com.desafio.gestaopessoas;

import br.com.desafio.gestaopessoas.application.DeactivationResult;
import br.com.desafio.gestaopessoas.application.EmployeeDeactivation;
import br.com.desafio.gestaopessoas.application.DeactivateEmployee;
import br.com.desafio.gestaopessoas.domain.employee.*;
import br.com.desafio.gestaopessoas.infrastructure.messaging.*;
import br.com.desafio.gestaopessoas.infrastructure.persistence.OutboxEventEntity;
import br.com.desafio.gestaopessoas.infrastructure.persistence.OutboxEventJpaRepository;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import br.com.desafio.gestaopessoas.support.WithIntegrationContainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WithIntegrationContainers
@SpringBootTest
class EmployeeStatusOutboxIntegrationTest {
    @Autowired EmployeeDeactivation deactivation;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Autowired EmployeeEventHistoryHandler handler;
    @MockitoSpyBean DeactivateEmployee useCase;
    @MockitoSpyBean OutboxEventJpaRepository events;
    private final UUID department = UUID.randomUUID();
    private final UUID position = UUID.randomUUID();
    private final UUID employee = UUID.randomUUID();

    @BeforeEach void setup() {
        jdbc.update("insert into departments(id,name,cost_center_code) values(?,?,?)", department, "Status", "test");
        jdbc.update("insert into positions(id,title,level) values(?,?,?)", position, "Dev", "Senior");
        jdbc.update("""
                insert into employees(id,name,email,department_id,position_id,created_at,status)
                values(?,?,?,?,?,current_timestamp,'ACTIVE')
                """, employee, "Teste", employee + "@example.com", department, position);
    }

    @AfterEach void cleanup() {
        jdbc.update("delete from employee_events where employee_id=?", employee);
        jdbc.update("delete from outbox_events where employee_id=?", employee);
        jdbc.update("delete from employees where id=?", employee);
        jdbc.update("delete from positions where id=?", position);
        jdbc.update("delete from departments where id=?", department);
    }

    @Test void failureAfterEventInsertRollsBackStatusVersionAndOutbox() {
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("failure after outbox insert");
        }).when(useCase).execute(employee);

        assertThrows(IllegalStateException.class, () -> deactivation.execute(employee));
        assertEquals("ACTIVE", jdbc.queryForObject("select status from employees where id=?", String.class, employee));
        assertEquals(0L, jdbc.queryForObject("select version from employees where id=?", Long.class, employee));
        assertEquals(0, jdbc.queryForObject("select count(*) from outbox_events where employee_id=?", Integer.class, employee));
        reset(useCase);
        assertEquals(DeactivationResult.DEACTIVATED, deactivation.execute(employee));
        assertEquals(1, jdbc.queryForObject("select count(*) from outbox_events where employee_id=?", Integer.class, employee));
    }

    @Test void outboxPersistenceFailureRollsBackEmployee() {
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("outbox unavailable"))
                .when(events).saveAndFlush(any(OutboxEventEntity.class));
        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class, () -> deactivation.execute(employee));
        assertEquals("ACTIVE", jdbc.queryForObject("select status from employees where id=?", String.class, employee));
        assertEquals(0L, jdbc.queryForObject("select version from employees where id=?", Long.class, employee));
        assertEquals(0, jdbc.queryForObject("select count(*) from outbox_events where employee_id=?", Integer.class, employee));
    }

    @Test void statusMessageIsCompleteAndDuplicateDoesNotOverwriteHistory() {
        assertEquals(DeactivationResult.DEACTIVATED, deactivation.execute(employee));
        String payload = jdbc.queryForObject("select payload from outbox_events where employee_id=?", String.class, employee);
        var event = json.readValue(payload, EmployeeStatusChangedMessage.class);
        assertEquals(employee, event.employeeId());
        assertEquals(EmployeeStatus.ACTIVE, event.previousStatus());
        assertEquals(EmployeeStatus.INACTIVE, event.newStatus());
        assertNotEquals(java.time.Instant.EPOCH, event.occurredAt());
        assertFalse(payload.contains("@example.com"));
        var consumer = new EmployeeStatusChangedKafkaConsumer(json, handler);
        var record = new ConsumerRecord<>("status-test", 0, 0, employee.toString(), payload);
        consumer.consume(record);
        consumer.consume(record);
        assertEquals(1, jdbc.queryForObject("select count(*) from employee_events where employee_id=?", Integer.class, employee));
        assertEquals(payload, jdbc.queryForObject("select payload from employee_events where employee_id=?", String.class, employee));
        assertThrows(IllegalArgumentException.class,
                () -> consumer.consume(new ConsumerRecord<>("status-test", 0, 1, "wrong-key", payload)));
        assertEquals(DeactivationResult.ALREADY_INACTIVE, deactivation.execute(employee));
        assertEquals(1, jdbc.queryForObject("select count(*) from outbox_events where employee_id=?", Integer.class, employee));
    }
}
