package br.com.desafio.gestaopessoas.infrastructure.messaging;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class OutboxPublishingTransaction {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublishingTransaction.class);
    private final JdbcTemplate jdbc;
    private final KafkaOutboxDelivery delivery;
    private final Clock clock;
    private final long retryDelayMs;

    public OutboxPublishingTransaction(JdbcTemplate jdbc, KafkaOutboxDelivery delivery, Clock clock,
                                      @Value("${app.outbox.retry-delay-ms:5000}") long retryDelayMs) {
        this.jdbc = jdbc;
        this.delivery = delivery;
        this.clock = clock;
        this.retryDelayMs = retryDelayMs;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean publish(UUID eventId) {
        OffsetDateTime now = clock.instant().atOffset(ZoneOffset.UTC);
        var pending = jdbc.query("""
                SELECT event_id, employee_id, payload FROM outbox_events
                WHERE event_id = ? AND published_at IS NULL AND available_at <= ? FOR UPDATE
                """, (rs, row) -> new Pending(rs.getObject("event_id", UUID.class),
                rs.getObject("employee_id", UUID.class), rs.getString("payload")), eventId, now);
        if (pending.isEmpty()) return false;
        Pending event = pending.getFirst();
        try {
            delivery.send(event.employeeId(), event.payload());
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            // Não persiste payload nem mensagens de erro potencialmente sensíveis.
            jdbc.update("""
                    UPDATE outbox_events SET attempts = attempts + 1, available_at = ?, last_error = ?
                    WHERE event_id = ?
                    """, clock.instant().plusMillis(retryDelayMs).atOffset(ZoneOffset.UTC),
                    failure.getClass().getSimpleName(), event.eventId());
            log.warn("outbox_publish_failed eventId={} failureType={}",
                    event.eventId(), failure.getClass().getSimpleName());
            return false;
        }
        // Falha de banco aqui causa rollback e reenvio; não tenta desfazer o envio Kafka.
        jdbc.update("""
                UPDATE outbox_events SET published_at = ?, attempts = attempts + 1, last_error = NULL
                WHERE event_id = ?
                """, clock.instant().atOffset(ZoneOffset.UTC), event.eventId());
        log.info("outbox_published eventId={} employeeId={}", event.eventId(), event.employeeId());
        return true;
    }

    private record Pending(UUID eventId, UUID employeeId, String payload) {}
}
