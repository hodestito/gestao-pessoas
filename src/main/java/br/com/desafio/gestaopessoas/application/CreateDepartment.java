package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.department.Department;

public class CreateDepartment {
    private final DepartmentRepository repository;
    public CreateDepartment(DepartmentRepository repository) { this.repository = repository; }
    public Department execute(String name, String code) { return repository.save(Department.create(name, code)); }
}
