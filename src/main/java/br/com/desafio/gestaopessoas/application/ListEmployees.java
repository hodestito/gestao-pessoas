package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class ListEmployees {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    private final EmployeeRepository employees;

    public ListEmployees(EmployeeRepository employees) { this.employees = employees; }

    public Page<Employee> execute(String name, EmployeeStatus status, int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page deve ser maior ou igual a zero");
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size deve estar entre 1 e 100");
        if ((long) page * size > Integer.MAX_VALUE) throw new IllegalArgumentException("deslocamento de página excede o limite suportado");
        String normalizedName = name == null || name.isBlank() ? null : name.strip();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending().and(Sort.by("id").ascending()));
        return employees.findAll(normalizedName, status, pageable);
    }
}
