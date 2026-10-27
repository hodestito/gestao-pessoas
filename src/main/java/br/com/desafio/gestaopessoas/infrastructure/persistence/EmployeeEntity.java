package br.com.desafio.gestaopessoas.infrastructure.persistence;

import br.com.desafio.gestaopessoas.domain.employee.Email;
import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employees")
public class EmployeeEntity {
    @Id UUID id;
    @Column(nullable = false, length = 120) String name;
    @Column(nullable = false, length = 254) String email;
    @Column(name = "department_id", nullable = false) UUID departmentId;
    @Column(name = "position_id", nullable = false) UUID positionId;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) EmployeeStatus status;
    @Version @Column(nullable = false) Long version;

    protected EmployeeEntity() {}

    EmployeeEntity(Employee employee) {
        id = employee.id(); name = employee.name(); email = employee.email().value();
        departmentId = employee.departmentId(); positionId = employee.positionId();
        createdAt = employee.createdAt(); status = employee.status();
    }

    EmployeeEntity(Employee employee, long version) {
        this(employee);
        this.version = version;
    }

    Employee toDomain() {
        return Employee.restore(id, name, new Email(email), departmentId, positionId, createdAt, status);
    }
}
