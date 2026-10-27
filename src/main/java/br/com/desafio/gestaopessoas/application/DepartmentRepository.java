package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.department.Department;
import java.util.UUID;

public interface DepartmentRepository {
    Department save(Department department);
    boolean existsById(UUID id);
}
