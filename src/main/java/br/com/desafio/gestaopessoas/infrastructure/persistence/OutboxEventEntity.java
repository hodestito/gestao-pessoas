package br.com.desafio.gestaopessoas.infrastructure.persistence;

import br.com.desafio.gestaopessoas.infrastructure.messaging.EmployeeCreatedMessage;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity implements Persistable<UUID> {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "event_type", nullable = false, length = 80) private String eventType;
    @Column(name = "schema_version", nullable = false) private int schemaVersion;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(nullable = false, columnDefinition = "text") private String payload;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(nullable = false) private int attempts;
    @Column(name = "available_at", nullable = false) private Instant availableAt;
    @Column(name = "last_error", length = 120) private String lastError;
    @Transient private boolean newEntity = true;

    protected OutboxEventEntity() {}

    public OutboxEventEntity(EmployeeCreatedMessage event, String payload) {
        eventId = event.eventId();
        employeeId = event.employeeId();
        eventType = event.eventType();
        schemaVersion = event.schemaVersion();
        occurredAt = event.occurredAt();
        availableAt = occurredAt;
        this.payload = payload;
    }

    @Override public UUID getId() { return eventId; }
    @Override public boolean isNew() { return newEntity; }
    @PostPersist @PostLoad private void markPersisted() { newEntity = false; }

    public UUID employeeId() { return employeeId; }
    public String payload() { return payload; }

    public void markPublished(Instant now) {
        publishedAt = now;
        attempts++;
        lastError = null;
    }

    public void reschedule(Instant availableAt, String errorType) {
        attempts++;
        this.availableAt = availableAt;
        lastError = errorType;
    }
}
