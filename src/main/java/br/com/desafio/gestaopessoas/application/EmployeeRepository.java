package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository {
    Employee save(Employee employee);
    /** As consultas retornam resultados em createdAt ASC, com desempate por id ASC. */
    PageResult<Employee> findAll(String name, EmployeeStatus status, EmployeePagination pagination);
    /** Mesma ordenação e paginação da listagem geral. */
    PageResult<Employee> findByDepartmentName(String department, EmployeePagination pagination);
    Optional<VersionedEmployee> findVersionedById(UUID id);
    void saveVersioned(VersionedEmployee employee);
}
