package br.com.desafio.gestaopessoas.infrastructure.messaging;

import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatusChangedEvent;
import java.time.Instant;
import java.util.UUID;

public record EmployeeStatusChangedMessage(int schemaVersion, String eventType, UUID eventId,
        UUID employeeId, Instant occurredAt, EmployeeStatus previousStatus, EmployeeStatus newStatus)
        implements EmployeeEventMessage {
    public static final String TYPE = "EmployeeStatusChangedEvent";

    public EmployeeStatusChangedMessage {
        if (schemaVersion != 1 || !TYPE.equals(eventType)) {
            throw new IllegalArgumentException("Unsupported employee status event contract");
        }
        new EmployeeStatusChangedEvent(eventId, employeeId, occurredAt, previousStatus, newStatus);
    }

    public static EmployeeStatusChangedMessage from(EmployeeStatusChangedEvent event) {
        return new EmployeeStatusChangedMessage(1, TYPE, event.eventId(), event.employeeId(), event.occurredAt(),
                event.previousStatus(), event.newStatus());
    }
}
