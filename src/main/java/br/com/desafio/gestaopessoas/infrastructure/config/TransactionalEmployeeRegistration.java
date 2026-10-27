package br.com.desafio.gestaopessoas.infrastructure.config;

import br.com.desafio.gestaopessoas.application.CreateEmployee;
import br.com.desafio.gestaopessoas.application.EmployeeRegistration;
import br.com.desafio.gestaopessoas.domain.employee.Employee;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionalEmployeeRegistration implements EmployeeRegistration {
    private final CreateEmployee useCase;
    public TransactionalEmployeeRegistration(CreateEmployee useCase) { this.useCase = useCase; }

    @Transactional
    public Employee execute(String name, String email, UUID departmentId, UUID positionId) {
        return useCase.execute(name, email, departmentId, positionId);
    }
}
