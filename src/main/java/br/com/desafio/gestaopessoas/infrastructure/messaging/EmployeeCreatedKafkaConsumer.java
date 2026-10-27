package br.com.desafio.gestaopessoas.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class EmployeeCreatedKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(EmployeeCreatedKafkaConsumer.class);
    private final JsonMapper json;
    private final EmployeeHistoryWriter history;

    public EmployeeCreatedKafkaConsumer(JsonMapper json, EmployeeHistoryWriter history) {
        this.json = json;
        this.history = history;
    }

    @KafkaListener(id = "employee-created-history", topics = "${app.messaging.topic}",
                   groupId = "${app.messaging.group-id}")
    public void consume(ConsumerRecord<String, String> record) {
        EmployeeCreatedMessage event = json.readValue(record.value(), EmployeeCreatedMessage.class);
        if (!event.employeeId().toString().equals(record.key())) {
            throw new IllegalArgumentException("Employee event key does not match payload");
        }
        try {
            history.record(event, record.value());
        } catch (DuplicateKeyException duplicate) {
            // A transação do writer já sofreu rollback antes de chegar aqui.
            log.info("employee_event_duplicate eventId={} offset={}", event.eventId(), record.offset());
            return;
        }
        log.info("employee_created_consumed eventId={} employeeId={} partition={} offset={}",
                event.eventId(), event.employeeId(), record.partition(), record.offset());
    }
}
