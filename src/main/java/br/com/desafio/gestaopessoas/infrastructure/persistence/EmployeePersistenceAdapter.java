package br.com.desafio.gestaopessoas.infrastructure.persistence;

import br.com.desafio.gestaopessoas.application.EmployeeRepository;
import br.com.desafio.gestaopessoas.domain.employee.Email;
import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeePersistenceAdapter implements EmployeeRepository {
    private final EmployeeJpaRepository repository;
    EmployeePersistenceAdapter(EmployeeJpaRepository repository) { this.repository = repository; }
    public boolean existsByEmail(Email email) { return repository.existsByEmail(email.value()); }
    public Employee save(Employee employee) { return repository.saveAndFlush(new EmployeeEntity(employee)).toDomain(); }

    public Page<Employee> findByDepartmentName(String department, Pageable pageable) {
        return repository.searchByDepartment(department, pageable).map(EmployeeEntity::toDomain);
    }

    public Page<Employee> findAll(String name, EmployeeStatus status, Pageable pageable) {
        Specification<EmployeeEntity> filters = (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (name != null) {
                String escapedName = name.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
                predicate = builder.and(predicate, builder.like(builder.lower(root.get("name")), "%" + escapedName + "%", '!'));
            }
            if (status != null) predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            return predicate;
        };
        return repository.findAll(filters, pageable).map(EmployeeEntity::toDomain);
    }
}
