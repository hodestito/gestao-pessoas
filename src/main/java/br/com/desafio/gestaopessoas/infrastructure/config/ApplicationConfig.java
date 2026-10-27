package br.com.desafio.gestaopessoas.infrastructure.config;

import br.com.desafio.gestaopessoas.application.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Bean CreateDepartment createDepartment(DepartmentRepository repository) { return new CreateDepartment(repository); }
    @Bean CreatePosition createPosition(PositionRepository repository) { return new CreatePosition(repository); }
}
