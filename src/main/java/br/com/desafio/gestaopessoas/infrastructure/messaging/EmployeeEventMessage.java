package br.com.desafio.gestaopessoas.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

/** Metadados comuns de transporte; cada tópico mantém seu contrato concreto. */
public sealed interface EmployeeEventMessage permits EmployeeCreatedMessage, EmployeeStatusChangedMessage {
    int schemaVersion();
    String eventType();
    UUID eventId();
    UUID employeeId();
    Instant occurredAt();
}
