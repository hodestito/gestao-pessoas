package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Employee;
import org.springframework.data.domain.Page;

public class SearchEmployeesByDepartment {
    private final EmployeeRepository employees;

    public SearchEmployeesByDepartment(EmployeeRepository employees) { this.employees = employees; }

    public Page<Employee> execute(String department, int page, int size) {
        if (department == null || department.isBlank()) {
            throw new IllegalArgumentException("department é obrigatório");
        }
        return employees.findByDepartmentName(department.strip(), EmployeePagination.create(page, size));
    }
}
