package br.com.desafio.gestaopessoas.infrastructure.persistence;

import br.com.desafio.gestaopessoas.domain.employee.Email;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeRegistrationRequirements;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmployeeRegistrationRequirements implements EmployeeRegistrationRequirements {
    private final DepartmentJpaRepository departments;
    private final PositionJpaRepository positions;
    private final EmployeeJpaRepository employees;

    public JpaEmployeeRegistrationRequirements(DepartmentJpaRepository departments,
                                               PositionJpaRepository positions, EmployeeJpaRepository employees) {
        this.departments = departments;
        this.positions = positions;
        this.employees = employees;
    }

    @Override public boolean departmentExists(UUID departmentId) { return departments.existsById(departmentId); }
    @Override public boolean positionExists(UUID positionId) { return positions.existsById(positionId); }
    @Override public boolean emailAvailable(Email email) { return !employees.existsByEmail(email.value()); }
}
