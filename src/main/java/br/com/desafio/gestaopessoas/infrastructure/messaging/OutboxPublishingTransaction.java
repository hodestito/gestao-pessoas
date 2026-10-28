package br.com.desafio.gestaopessoas.infrastructure.messaging;

import br.com.desafio.gestaopessoas.infrastructure.persistence.OutboxEventJpaRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class OutboxPublishingTransaction {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublishingTransaction.class);
    private final OutboxEventJpaRepository repository;
    private final KafkaOutboxDelivery delivery;
    private final Clock clock;
    private final long retryDelayMs;

    public OutboxPublishingTransaction(OutboxEventJpaRepository repository, KafkaOutboxDelivery delivery, Clock clock,
                                      @Value("${app.outbox.retry-delay-ms:5000}") long retryDelayMs) {
        this.repository = repository;
        this.delivery = delivery;
        this.clock = clock;
        this.retryDelayMs = retryDelayMs;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean publish(UUID eventId) {
        var pending = repository.findPendingForUpdate(eventId, clock.instant());
        if (pending.isEmpty()) return false;
        var event = pending.orElseThrow();
        try {
            delivery.send(event.eventType(), event.employeeId(), event.payload());
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            // Não persiste payload nem mensagens de erro potencialmente sensíveis.
            event.reschedule(clock.instant().plusMillis(retryDelayMs), failure.getClass().getSimpleName());
            repository.flush();
            log.warn("outbox_publish_failed eventId={} failureType={}",
                    event.getId(), failure.getClass().getSimpleName());
            return false;
        }
        // Falha de banco aqui causa rollback e reenvio; não tenta desfazer o envio Kafka.
        event.markPublished(clock.instant());
        repository.flush();
        log.info("outbox_published eventId={} employeeId={}", event.getId(), event.employeeId());
        return true;
    }
}
