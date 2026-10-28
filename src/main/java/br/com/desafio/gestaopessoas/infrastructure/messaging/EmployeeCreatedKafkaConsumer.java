package br.com.desafio.gestaopessoas.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class EmployeeCreatedKafkaConsumer {
    private final JsonMapper json;
    private final EmployeeEventHistoryHandler handler;

    public EmployeeCreatedKafkaConsumer(JsonMapper json, EmployeeEventHistoryHandler handler) {
        this.json = json;
        this.handler = handler;
    }

    @KafkaListener(id = "employee-created-history", topics = "${app.messaging.topic}",
                   groupId = "${app.messaging.group-id}")
    public void consume(ConsumerRecord<String, String> record) {
        handler.handle(json.readValue(record.value(), EmployeeCreatedMessage.class), record);
    }
}
