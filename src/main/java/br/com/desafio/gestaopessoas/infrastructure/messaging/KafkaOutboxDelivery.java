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
    private final String statusTopic;

    public KafkaOutboxDelivery(KafkaTemplate<String, String> kafka,
                               @Value("${app.messaging.topic}") String topic,
                               @Value("${app.messaging.status-topic}") String statusTopic) {
        this.kafka = kafka;
        this.topic = topic;
        this.statusTopic = statusTopic;
    }

    public void send(String eventType, UUID employeeId, String payload) throws Exception {
        String destination = switch (eventType) {
            case EmployeeCreatedMessage.TYPE -> topic;
            case EmployeeStatusChangedMessage.TYPE -> statusTopic;
            default -> throw new IllegalArgumentException("Unsupported outbox event type");
        };
        // A marcação na Outbox só acontece após confirmação do broker.
        kafka.send(destination, employeeId.toString(), payload).get(15, TimeUnit.SECONDS);
    }
}
