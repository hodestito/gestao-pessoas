package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Email;
import br.com.desafio.gestaopessoas.domain.employee.Employee;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import static br.com.desafio.gestaopessoas.application.EmployeeRegistrationException.Reason.*;

public class CreateEmployee {
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final Clock clock;
    private final Supplier<UUID> ids;

    public CreateEmployee(EmployeeRepository employees, DepartmentRepository departments,
                          PositionRepository positions, Clock clock, Supplier<UUID> ids) {
        this.employees = employees;
        this.departments = departments;
        this.positions = positions;
        this.clock = clock;
        this.ids = ids;
    }

    public Employee execute(String name, String address, UUID departmentId, UUID positionId) {
        Email email = new Email(address);
        Employee employee = Employee.create(ids.get(), name, email, departmentId, positionId, Instant.now(clock));
        if (!departments.existsById(departmentId)) throw new EmployeeRegistrationException(DEPARTMENT_NOT_FOUND);
        if (!positions.existsById(positionId)) throw new EmployeeRegistrationException(POSITION_NOT_FOUND);
        if (employees.existsByEmail(email)) throw new EmployeeRegistrationException(EMAIL_ALREADY_EXISTS);
        return employees.save(employee);
    }
}
