package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateEmployeeTest {
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final PositionRepository positions = mock(PositionRepository.class);
    private final EmployeeEventOutbox outbox = mock(EmployeeEventOutbox.class);
    private final UUID id = UUID.fromString("8f9eb9f2-ab74-4dd7-a77d-458a127f96bc");
    private final UUID eventId = UUID.fromString("dfd08901-d58a-4787-8225-974ff2441c7d");
    private final UUID department = UUID.randomUUID();
    private final UUID position = UUID.randomUUID();
    private final Instant timestamp = Instant.parse("2026-10-27T12:00:00Z");
    private CreateEmployee useCase;

    @BeforeEach void setup() {
        var sequence = new java.util.concurrent.atomic.AtomicInteger();
        useCase = new CreateEmployee(employees, departments, positions, Clock.fixed(timestamp, ZoneOffset.UTC),
                () -> sequence.getAndIncrement() == 0 ? id : eventId, outbox);
        when(departments.existsById(department)).thenReturn(true);
        when(positions.existsById(position)).thenReturn(true);
    }

    @Test void createsActiveEmployeeWithNormalizedEmailAndInjectedIdentityAndTime() {
        when(employees.save(any())).thenAnswer(i -> i.getArgument(0));
        Employee result = useCase.execute("Hugo", " HUGO@EXAMPLE.COM ", department, position);
        assertEquals(id, result.id());
        assertEquals(timestamp, result.createdAt());
        assertEquals("hugo@example.com", result.email().value());
        assertEquals(EmployeeStatus.ACTIVE, result.status());
        var order = inOrder(employees, outbox);
        order.verify(employees).save(any());
        order.verify(outbox).append(new br.com.desafio.gestaopessoas.domain.employee.EmployeeCreatedEvent(eventId, id, timestamp));
    }

    @Test void refusesMissingDepartmentBeforeSaving() {
        when(departments.existsById(department)).thenReturn(false);
        var error = assertThrows(EmployeeRegistrationException.class, () -> useCase.execute("Hugo", "hugo@example.com", department, position));
        assertEquals(EmployeeRegistrationException.Reason.DEPARTMENT_NOT_FOUND, error.reason());
        verifyNoInteractions(employees, outbox);
    }

    @Test void refusesMissingPositionBeforeSaving() {
        when(positions.existsById(position)).thenReturn(false);
        var error = assertThrows(EmployeeRegistrationException.class, () -> useCase.execute("Hugo", "hugo@example.com", department, position));
        assertEquals(EmployeeRegistrationException.Reason.POSITION_NOT_FOUND, error.reason());
        verifyNoInteractions(employees, outbox);
    }

    @Test void refusesDuplicateEmailBeforeSaving() {
        when(employees.existsByEmail(any())).thenReturn(true);
        var error = assertThrows(EmployeeRegistrationException.class, () -> useCase.execute("Hugo", "HUGO@EXAMPLE.COM", department, position));
        assertEquals(EmployeeRegistrationException.Reason.EMAIL_ALREADY_EXISTS, error.reason());
        verify(employees, never()).save(any());
        verifyNoInteractions(outbox);
    }

    @Test void refusesInvalidEmailBeforeQueryingOrSaving() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("Hugo", "invalid", department, position));
        verifyNoInteractions(employees, departments, positions, outbox);
    }

    @Test void persistenceFailureDoesNotAppendEvent() {
        when(employees.save(any())).thenThrow(new IllegalStateException("persistence failed"));
        assertThrows(IllegalStateException.class, () -> useCase.execute("Hugo", "hugo@example.com", department, position));
        verifyNoInteractions(outbox);
    }

    @Test void outboxFailureIsPropagatedSoTransactionCanRollback() {
        when(employees.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new IllegalStateException("outbox failed")).when(outbox).append(any());
        assertThrows(IllegalStateException.class, () -> useCase.execute("Hugo", "hugo@example.com", department, position));
    }
}
