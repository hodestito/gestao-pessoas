package br.com.desafio.gestaopessoas.infrastructure.config;

import br.com.desafio.gestaopessoas.application.*;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeRegistrationPolicy;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeRegistrationRequirements;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Bean CreateDepartment createDepartment(DepartmentRepository repository) { return new CreateDepartment(repository); }
    @Bean CreatePosition createPosition(PositionRepository repository) { return new CreatePosition(repository); }
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean EmployeeRegistrationPolicy employeeRegistrationPolicy(EmployeeRegistrationRequirements requirements) {
        return new EmployeeRegistrationPolicy(requirements);
    }
    @Bean CreateEmployee createEmployee(EmployeeRepository employees, EmployeeRegistrationPolicy policy, Clock clock, EmployeeEventOutbox outbox) {
        return new CreateEmployee(employees, policy, clock, UUID::randomUUID, outbox);
    }
    @Bean ListEmployees listEmployees(EmployeeRepository employees) { return new ListEmployees(employees); }
    @Bean SearchEmployeesByDepartment searchEmployeesByDepartment(EmployeeRepository employees) {
        return new SearchEmployeesByDepartment(employees);
    }
    @Bean DeactivateEmployee deactivateEmployee(EmployeeRepository employees, EmployeeEventOutbox outbox, Clock clock) {
        return new DeactivateEmployee(employees, outbox, clock, UUID::randomUUID);
    }
}
