package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Email;
import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatusChangedEvent;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeactivateEmployeeTest {
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final EmployeeEventOutbox outbox = mock(EmployeeEventOutbox.class);
    private final UUID eventId = UUID.randomUUID();
    private final Instant timestamp = Instant.parse("2026-10-28T10:00:00Z");
    private final DeactivateEmployee useCase = new DeactivateEmployee(employees, outbox,
            Clock.fixed(timestamp, ZoneOffset.UTC), () -> eventId);
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
        var order = inOrder(employees, outbox);
        order.verify(employees).saveVersioned(snapshot);
        order.verify(outbox).append(new EmployeeStatusChangedEvent(eventId, id, timestamp,
                EmployeeStatus.ACTIVE, EmployeeStatus.INACTIVE));
    }

    @Test void alreadyInactiveDoesNotWrite() {
        Employee employee = employee();
        employee.deactivate();
        when(employees.findVersionedById(id)).thenReturn(Optional.of(new VersionedEmployee(employee, 1)));
        assertEquals(DeactivationResult.ALREADY_INACTIVE, useCase.execute(id));
        verify(employees, never()).saveVersioned(any());
        verifyNoInteractions(outbox);
    }

    @Test void missingEmployeeDoesNotWrite() {
        when(employees.findVersionedById(id)).thenReturn(Optional.empty());
        assertEquals(DeactivationResult.NOT_FOUND, useCase.execute(id));
        verify(employees, never()).saveVersioned(any());
        verifyNoInteractions(outbox);
    }

    @Test void versionConflictDoesNotAppendAnEvent() {
        when(employees.findVersionedById(id)).thenReturn(Optional.of(new VersionedEmployee(employee(), 7)));
        doThrow(new org.springframework.dao.OptimisticLockingFailureException("stale"))
                .when(employees).saveVersioned(any());
        assertThrows(org.springframework.dao.OptimisticLockingFailureException.class, () -> useCase.execute(id));
        verifyNoInteractions(outbox);
    }

    @Test void outboxFailurePropagatesToRollbackTheTransaction() {
        when(employees.findVersionedById(id)).thenReturn(Optional.of(new VersionedEmployee(employee(), 7)));
        doThrow(new IllegalStateException("outbox unavailable")).when(outbox).append(any(EmployeeStatusChangedEvent.class));
        assertThrows(IllegalStateException.class, () -> useCase.execute(id));
    }
}
