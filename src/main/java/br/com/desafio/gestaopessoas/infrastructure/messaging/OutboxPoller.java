package br.com.desafio.gestaopessoas.infrastructure.messaging;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = {"app.messaging.enabled", "app.outbox.scheduling-enabled"}, havingValue = "true")
public class OutboxPoller {
    private final OutboxPublishingTransaction publisher;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public OutboxPoller(OutboxPublishingTransaction publisher, JdbcTemplate jdbc, Clock clock) {
        this.publisher = publisher;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:1000}")
    public void publishPending() {
        var ids = jdbc.query("""
                SELECT event_id FROM outbox_events
                WHERE published_at IS NULL AND available_at <= ?
                ORDER BY occurred_at, event_id LIMIT 20
                """, (rs, row) -> rs.getObject("event_id", UUID.class), clock.instant().atOffset(ZoneOffset.UTC));
        for (UUID id : ids) {
            if (Thread.currentThread().isInterrupted()) return;
            publisher.publish(id);
        }
    }
}
