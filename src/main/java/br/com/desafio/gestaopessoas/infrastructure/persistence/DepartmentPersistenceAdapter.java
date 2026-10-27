package br.com.desafio.gestaopessoas.infrastructure.persistence;

import br.com.desafio.gestaopessoas.application.DepartmentRepository;
import br.com.desafio.gestaopessoas.domain.department.Department;
import org.springframework.stereotype.Repository;

@Repository
public class DepartmentPersistenceAdapter implements DepartmentRepository {
    private final DepartmentJpaRepository repository;
    DepartmentPersistenceAdapter(DepartmentJpaRepository repository) { this.repository = repository; }
    public Department save(Department department) {
        repository.save(new DepartmentEntity(department.id(), department.name(), department.costCenterCode().value()));
        return department;
    }
}
