package br.com.desafio.gestaopessoas.domain.employee;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EmployeeStatusChangedEvent(UUID eventId, UUID employeeId, Instant occurredAt,
                                         EmployeeStatus previousStatus, EmployeeStatus newStatus) {
    public EmployeeStatusChangedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(employeeId, "employeeId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(previousStatus, "previousStatus");
        Objects.requireNonNull(newStatus, "newStatus");
        if (previousStatus == newStatus) throw new IllegalArgumentException("Status must change");
    }
}
