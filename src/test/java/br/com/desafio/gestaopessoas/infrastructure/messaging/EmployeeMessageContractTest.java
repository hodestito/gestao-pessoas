package br.com.desafio.gestaopessoas.infrastructure.messaging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.ValueInstantiationException;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeMessageContractTest {
    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
    private final EmployeeHistoryWriter history = mock(EmployeeHistoryWriter.class);
    private final EmployeeEventHistoryHandler handler = new EmployeeEventHistoryHandler(history);
    private final UUID employeeId = UUID.randomUUID();

    @Test void creationConsumerRejectsUnsupportedVersionOrWrongEventTypeBeforeHistory() {
        var consumer = new EmployeeCreatedKafkaConsumer(json, handler);
        assertInvalidContracts(consumer::consume, payload(EmployeeCreatedMessage.TYPE), EmployeeStatusChangedMessage.TYPE);
    }

    @Test void statusConsumerRejectsUnsupportedVersionOrWrongEventTypeBeforeHistory() {
        var consumer = new EmployeeStatusChangedKafkaConsumer(json, handler);
        var data = payload(EmployeeStatusChangedMessage.TYPE);
        data.put("previousStatus", "ACTIVE");
        data.put("newStatus", "INACTIVE");
        assertInvalidContracts(consumer::consume, data, EmployeeCreatedMessage.TYPE);
    }

    private Map<String, Object> payload(String type) {
        var data = new LinkedHashMap<String, Object>();
        data.put("schemaVersion", 1);
        data.put("eventType", type);
        data.put("eventId", UUID.randomUUID().toString());
        data.put("employeeId", employeeId.toString());
        data.put("occurredAt", "2026-10-28T12:00:00Z");
        return data;
    }

    private void assertInvalidContracts(Consumer<ConsumerRecord<String, String>> consumer,
                                        Map<String, Object> validData, String wrongType) {
        for (var change : Map.<String, Object>of("schemaVersion", 2, "eventType", wrongType).entrySet()) {
            var invalid = new LinkedHashMap<>(validData);
            invalid.put(change.getKey(), change.getValue());
            var record = new ConsumerRecord<>("test", 0, 0, employeeId.toString(), json.writeValueAsString(invalid));
            var error = assertThrows(ValueInstantiationException.class, () -> consumer.accept(record));
            assertInstanceOf(IllegalArgumentException.class, error.getCause());
        }
        verifyNoInteractions(history);
    }
}
