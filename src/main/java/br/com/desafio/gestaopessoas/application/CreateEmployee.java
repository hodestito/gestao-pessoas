package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Email;
import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeCreatedEvent;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeRegistrationPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

public class CreateEmployee {
    private final EmployeeRepository employees;
    private final EmployeeRegistrationPolicy policy;
    private final Clock clock;
    private final Supplier<UUID> ids;
    private final EmployeeEventOutbox outbox;

    public CreateEmployee(EmployeeRepository employees, EmployeeRegistrationPolicy policy,
                          Clock clock, Supplier<UUID> ids, EmployeeEventOutbox outbox) {
        this.employees = employees;
        this.policy = policy;
        this.clock = clock;
        this.ids = ids;
        this.outbox = outbox;
    }

    public Employee execute(String name, String address, UUID departmentId, UUID positionId) {
        Email email = new Email(address);
        Employee employee = Employee.create(ids.get(), name, email, departmentId, positionId, Instant.now(clock));
        policy.validate(email, departmentId, positionId);
        Employee saved = employees.save(employee);
        outbox.append(new EmployeeCreatedEvent(ids.get(), saved.id(), saved.createdAt()));
        return saved;
    }
}
