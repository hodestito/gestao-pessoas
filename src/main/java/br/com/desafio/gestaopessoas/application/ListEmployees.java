package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;

public class ListEmployees {
    private final EmployeeRepository employees;

    public ListEmployees(EmployeeRepository employees) { this.employees = employees; }

    public PageResult<Employee> execute(String name, EmployeeStatus status, int page, int size) {
        String normalizedName = name == null || name.isBlank() ? null : name.strip();
        return employees.findAll(normalizedName, status, EmployeePagination.create(page, size));
    }
}
