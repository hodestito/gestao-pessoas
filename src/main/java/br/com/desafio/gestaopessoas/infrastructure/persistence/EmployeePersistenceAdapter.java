package br.com.desafio.gestaopessoas.infrastructure.persistence;

import br.com.desafio.gestaopessoas.application.EmployeeRepository;
import br.com.desafio.gestaopessoas.domain.employee.Email;
import br.com.desafio.gestaopessoas.domain.employee.Employee;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeePersistenceAdapter implements EmployeeRepository {
    private final EmployeeJpaRepository repository;
    EmployeePersistenceAdapter(EmployeeJpaRepository repository) { this.repository = repository; }
    public boolean existsByEmail(Email email) { return repository.existsByEmail(email.value()); }
    public Employee save(Employee employee) { return repository.saveAndFlush(new EmployeeEntity(employee)).toDomain(); }
}
