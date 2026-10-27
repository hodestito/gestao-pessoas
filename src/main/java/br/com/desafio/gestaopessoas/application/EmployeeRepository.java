package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Email;
import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeRepository {
    boolean existsByEmail(Email email);
    Employee save(Employee employee);
    Page<Employee> findAll(String name, EmployeeStatus status, Pageable pageable);
    Page<Employee> findByDepartmentName(String department, Pageable pageable);
}
