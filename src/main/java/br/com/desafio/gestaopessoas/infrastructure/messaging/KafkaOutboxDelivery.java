package br.com.desafio.gestaopessoas.infrastructure.messaging;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class KafkaOutboxDelivery {
    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    public KafkaOutboxDelivery(KafkaTemplate<String, String> kafka,
                               @Value("${app.messaging.topic}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    public void send(UUID employeeId, String payload) throws Exception {
        // A marcação na Outbox só acontece após confirmação do broker.
        kafka.send(topic, employeeId.toString(), payload).get(15, TimeUnit.SECONDS);
    }
}
