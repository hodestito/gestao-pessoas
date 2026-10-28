package br.com.desafio.gestaopessoas.infrastructure.messaging;

import br.com.desafio.gestaopessoas.infrastructure.persistence.OutboxEventJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboxPollerTest {
    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
    private final OutboxPublishingTransaction publisher = mock(OutboxPublishingTransaction.class);
    private final Instant now = Instant.parse("2026-10-28T12:00:00Z");
    private final OutboxPoller poller = new OutboxPoller(publisher, repository, Clock.fixed(now, ZoneOffset.UTC));

    @Test void deferredEventDoesNotPreventOtherSelectedEventsFromPublishing() {
        UUID deferred = UUID.randomUUID();
        UUID ready = UUID.randomUUID();
        when(repository.findAvailableIds(eq(now), any())).thenReturn(List.of(deferred, ready));
        when(publisher.publish(deferred)).thenReturn(false);
        when(publisher.publish(ready)).thenReturn(true);
        poller.publishPending();
        var order = inOrder(publisher);
        order.verify(publisher).publish(deferred);
        order.verify(publisher).publish(ready);
        var page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAvailableIds(eq(now), page.capture());
        assertEquals(0, page.getValue().getPageNumber());
        assertEquals(20, page.getValue().getPageSize());
    }

    @Test void interruptionStopsBeforeStartingTheNextEvent() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(repository.findAvailableIds(eq(now), any())).thenReturn(List.of(first, second));
        when(publisher.publish(first)).thenAnswer(call -> {
            Thread.currentThread().interrupt();
            return true;
        });
        try {
            poller.publishPending();
            assertTrue(Thread.currentThread().isInterrupted());
            verify(publisher).publish(first);
            verify(publisher, never()).publish(second);
        } finally {
            Thread.interrupted();
        }
    }
}
