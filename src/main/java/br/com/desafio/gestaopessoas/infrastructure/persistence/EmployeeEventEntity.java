package br.com.desafio.gestaopessoas.infrastructure.persistence;

import br.com.desafio.gestaopessoas.infrastructure.messaging.EmployeeCreatedMessage;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "employee_events")
public class EmployeeEventEntity implements Persistable<UUID> {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "event_type", nullable = false, length = 80) private String eventType;
    @Column(name = "schema_version", nullable = false) private int schemaVersion;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;
    @Column(nullable = false, columnDefinition = "text") private String payload;
    @Transient private boolean newEntity = true;

    protected EmployeeEventEntity() {}

    public EmployeeEventEntity(EmployeeCreatedMessage event, Instant processedAt, String payload) {
        eventId = event.eventId();
        employeeId = event.employeeId();
        eventType = event.eventType();
        schemaVersion = event.schemaVersion();
        occurredAt = event.occurredAt();
        this.processedAt = processedAt;
        this.payload = payload;
    }

    @Override public UUID getId() { return eventId; }
    @Override public boolean isNew() { return newEntity; }
    @PostPersist @PostLoad private void markPersisted() { newEntity = false; }
}
