package br.com.desafio.gestaopessoas.domain.employee;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Fato imutável, sem dados pessoais nem dependências de transporte. */
public record EmployeeCreatedEvent(UUID eventId, UUID employeeId, Instant occurredAt) {
    public EmployeeCreatedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(employeeId, "employeeId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
