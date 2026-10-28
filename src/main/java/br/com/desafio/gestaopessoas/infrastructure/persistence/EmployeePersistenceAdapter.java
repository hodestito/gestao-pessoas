package br.com.desafio.gestaopessoas.infrastructure.persistence;

import br.com.desafio.gestaopessoas.application.EmployeeRepository;
import br.com.desafio.gestaopessoas.application.EmployeePagination;
import br.com.desafio.gestaopessoas.application.PageResult;
import br.com.desafio.gestaopessoas.application.VersionedEmployee;
import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeePersistenceAdapter implements EmployeeRepository {
    private final EmployeeJpaRepository repository;
    EmployeePersistenceAdapter(EmployeeJpaRepository repository) { this.repository = repository; }
    public Employee save(Employee employee) { return repository.saveAndFlush(new EmployeeEntity(employee)).toDomain(); }

    public Optional<VersionedEmployee> findVersionedById(UUID id) {
        return repository.findById(id).map(entity -> new VersionedEmployee(entity.toDomain(), entity.version));
    }

    public void saveVersioned(VersionedEmployee snapshot) {
        repository.saveAndFlush(new EmployeeEntity(snapshot.employee(), snapshot.version()));
    }

    public PageResult<Employee> findByDepartmentName(String department, EmployeePagination pagination) {
        return toResult(repository.searchByDepartment(department, toPageRequest(pagination)));
    }

    public PageResult<Employee> findAll(String name, EmployeeStatus status, EmployeePagination pagination) {
        Specification<EmployeeEntity> filters = (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (name != null) {
                String escapedName = name.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
                predicate = builder.and(predicate, builder.like(builder.lower(root.get("name")), "%" + escapedName + "%", '!'));
            }
            if (status != null) predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            return predicate;
        };
        return toResult(repository.findAll(filters, toPageRequest(pagination)));
    }

    private PageRequest toPageRequest(EmployeePagination pagination) {
        return PageRequest.of(pagination.page(), pagination.size(),
                Sort.by("createdAt").ascending().and(Sort.by("id").ascending()));
    }

    private PageResult<Employee> toResult(Page<EmployeeEntity> page) {
        return new PageResult<>(page.getContent().stream().map(EmployeeEntity::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
