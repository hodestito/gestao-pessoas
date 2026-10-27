package br.com.desafio.gestaopessoas.domain.employee;

import br.com.desafio.gestaopessoas.domain.RequiredText;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Entidade com identidade estável e transição explícita de estado. */
public final class Employee {
    private final UUID id;
    private final String name;
    private final Email email;
    private final UUID departmentId;
    private final UUID positionId;
    private final Instant createdAt;
    private EmployeeStatus status;

    private Employee(UUID id, String name, Email email, UUID departmentId, UUID positionId, Instant createdAt, EmployeeStatus status) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = RequiredText.normalize(name, "name", 120);
        this.email = Objects.requireNonNull(email, "email");
        this.departmentId = Objects.requireNonNull(departmentId, "departmentId");
        this.positionId = Objects.requireNonNull(positionId, "positionId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = Objects.requireNonNull(status, "status");
    }

    public static Employee create(UUID id, String name, Email email, UUID departmentId, UUID positionId, Instant createdAt) {
        return new Employee(id, name, email, departmentId, positionId, createdAt, EmployeeStatus.ACTIVE);
    }

    /** Reconstitui um estado persistido sem aplicar as regras de uma nova contratação. */
    public static Employee restore(UUID id, String name, Email email, UUID departmentId, UUID positionId, Instant createdAt, EmployeeStatus status) {
        return new Employee(id, name, email, departmentId, positionId, createdAt, status);
    }

    /** Retorna true apenas quando ocorreu uma transição. */
    public boolean deactivate() {
        if (status == EmployeeStatus.INACTIVE) return false;
        status = EmployeeStatus.INACTIVE;
        return true;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public Email email() { return email; }
    public UUID departmentId() { return departmentId; }
    public UUID positionId() { return positionId; }
    public Instant createdAt() { return createdAt; }
    public EmployeeStatus status() { return status; }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Employee employee && id.equals(employee.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}
