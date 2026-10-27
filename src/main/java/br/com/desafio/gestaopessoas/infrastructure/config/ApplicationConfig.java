package br.com.desafio.gestaopessoas.infrastructure.config;

import br.com.desafio.gestaopessoas.application.*;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Bean CreateDepartment createDepartment(DepartmentRepository repository) { return new CreateDepartment(repository); }
    @Bean CreatePosition createPosition(PositionRepository repository) { return new CreatePosition(repository); }
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean CreateEmployee createEmployee(EmployeeRepository employees, DepartmentRepository departments, PositionRepository positions, Clock clock) {
        return new CreateEmployee(employees, departments, positions, clock, UUID::randomUUID);
    }
}
