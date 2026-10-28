package br.com.desafio.gestaopessoas.infrastructure.messaging;

import br.com.desafio.gestaopessoas.domain.employee.EmployeeCreatedEvent;
import java.time.Instant;
import java.util.UUID;

/** Contrato JSON explícito; não usa nomes de classes Java como tipo Kafka. */
public record EmployeeCreatedMessage(int schemaVersion, String eventType, UUID eventId,
                                     UUID employeeId, Instant occurredAt) {
    public static final String TYPE = "EmployeeCreatedEvent";
    public EmployeeCreatedMessage {
        if (schemaVersion != 1 || !TYPE.equals(eventType)) {
            throw new IllegalArgumentException("Unsupported employee event contract");
        }
        new EmployeeCreatedEvent(eventId, employeeId, occurredAt);
    }
    public static EmployeeCreatedMessage from(EmployeeCreatedEvent event) {
        return new EmployeeCreatedMessage(1, TYPE, event.eventId(), event.employeeId(), event.occurredAt());
    }
}
