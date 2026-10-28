package br.com.desafio.gestaopessoas.infrastructure.messaging;

import br.com.desafio.gestaopessoas.infrastructure.persistence.OutboxEventJpaRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = {"app.messaging.enabled", "app.outbox.scheduling-enabled"}, havingValue = "true")
public class OutboxPoller {
    private final OutboxPublishingTransaction publisher;
    private final OutboxEventJpaRepository repository;
    private final Clock clock;

    public OutboxPoller(OutboxPublishingTransaction publisher, OutboxEventJpaRepository repository, Clock clock) {
        this.publisher = publisher;
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:1000}")
    public void publishPending() {
        var ids = repository.findAvailableIds(clock.instant(), PageRequest.of(0, 20));
        for (UUID id : ids) {
            if (Thread.currentThread().isInterrupted()) return;
            publisher.publish(id);
        }
    }
}
