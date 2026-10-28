package br.com.desafio.gestaopessoas.infrastructure.messaging;

import br.com.desafio.gestaopessoas.domain.employee.EmployeeCreatedEvent;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeCreatedKafkaConsumerTest {
    @Test void integrityFailureWithoutExistingEventIsNotTreatedAsDuplicate() {
        var json = JsonMapper.builder().findAndAddModules().build();
        var history = mock(EmployeeHistoryWriter.class);
        var event = EmployeeCreatedMessage.from(new EmployeeCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), Instant.now()));
        String payload = json.writeValueAsString(event);
        var failure = new DataIntegrityViolationException("not a duplicate");
        doThrow(failure).when(history).record(event, payload);
        var consumer = new EmployeeCreatedKafkaConsumer(json, new EmployeeEventHistoryHandler(history));
        assertSame(failure, assertThrows(DataIntegrityViolationException.class,
                () -> consumer.consume(new ConsumerRecord<>("test", 0, 0, event.employeeId().toString(), payload))));
        verify(history).contains(event.eventId());
    }
}
