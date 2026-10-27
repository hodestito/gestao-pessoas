package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Employee;
import java.util.UUID;

public interface EmployeeRegistration {
    Employee execute(String name, String email, UUID departmentId, UUID positionId);
}
