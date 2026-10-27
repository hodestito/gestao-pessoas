package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Email;
import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeactivateEmployeeTest {
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final DeactivateEmployee useCase = new DeactivateEmployee(employees);
    private final UUID id = UUID.randomUUID();

    private Employee employee() {
        return Employee.create(id, "Hugo", new Email("hugo@example.com"), UUID.randomUUID(), UUID.randomUUID(), Instant.EPOCH);
    }

    @Test void deactivatesActiveEmployeeWithTheVersionRead() {
        Employee employee = employee();
        var snapshot = new VersionedEmployee(employee, 7);
        when(employees.findVersionedById(id)).thenReturn(Optional.of(snapshot));
        assertEquals(DeactivationResult.DEACTIVATED, useCase.execute(id));
        assertEquals(EmployeeStatus.INACTIVE, employee.status());
        verify(employees).saveVersioned(snapshot);
    }

    @Test void alreadyInactiveDoesNotWrite() {
        Employee employee = employee();
        employee.deactivate();
        when(employees.findVersionedById(id)).thenReturn(Optional.of(new VersionedEmployee(employee, 1)));
        assertEquals(DeactivationResult.ALREADY_INACTIVE, useCase.execute(id));
        verify(employees, never()).saveVersioned(any());
    }

    @Test void missingEmployeeDoesNotWrite() {
        when(employees.findVersionedById(id)).thenReturn(Optional.empty());
        assertEquals(DeactivationResult.NOT_FOUND, useCase.execute(id));
        verify(employees, never()).saveVersioned(any());
    }
}
