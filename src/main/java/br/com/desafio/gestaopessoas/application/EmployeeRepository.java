package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Email;
import br.com.desafio.gestaopessoas.domain.employee.Employee;

public interface EmployeeRepository {
    boolean existsByEmail(Email email);
    Employee save(Employee employee);
}
