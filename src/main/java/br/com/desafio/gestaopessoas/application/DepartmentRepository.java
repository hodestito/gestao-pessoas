package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.department.Department;

public interface DepartmentRepository {
    Department save(Department department);
}
