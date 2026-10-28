package br.com.desafio.gestaopessoas.domain.employee;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeRegistrationPolicyTest {
    private final EmployeeRegistrationRequirements requirements = mock(EmployeeRegistrationRequirements.class);
    private final EmployeeRegistrationPolicy policy = new EmployeeRegistrationPolicy(requirements);
    private final UUID department = UUID.randomUUID();
    private final UUID position = UUID.randomUUID();
    private final Email email = new Email(" HUGO@EXAMPLE.COM ");

    @BeforeEach void setup() {
        when(requirements.departmentExists(department)).thenReturn(true);
        when(requirements.positionExists(position)).thenReturn(true);
        when(requirements.emailAvailable(email)).thenReturn(true);
    }

    @Test void admitsWhenReferencesExistAndNormalizedEmailIsAvailable() {
        assertDoesNotThrow(() -> policy.validate(email, department, position));
        var order = inOrder(requirements);
        order.verify(requirements).departmentExists(department);
        order.verify(requirements).positionExists(position);
        order.verify(requirements).emailAvailable(new Email("hugo@example.com"));
    }

    @Test void rejectsMissingDepartmentBeforeOtherChecks() {
        when(requirements.departmentExists(department)).thenReturn(false);
        var error = assertThrows(EmployeeRegistrationException.class, () -> policy.validate(email, department, position));
        assertEquals(EmployeeRegistrationException.Reason.DEPARTMENT_NOT_FOUND, error.reason());
        verify(requirements, never()).positionExists(any());
        verify(requirements, never()).emailAvailable(any());
    }

    @Test void rejectsMissingPositionBeforeCheckingEmail() {
        when(requirements.positionExists(position)).thenReturn(false);
        var error = assertThrows(EmployeeRegistrationException.class, () -> policy.validate(email, department, position));
        assertEquals(EmployeeRegistrationException.Reason.POSITION_NOT_FOUND, error.reason());
        verify(requirements, never()).emailAvailable(any());
    }

    @Test void rejectsUnavailableEmail() {
        when(requirements.emailAvailable(email)).thenReturn(false);
        var error = assertThrows(EmployeeRegistrationException.class, () -> policy.validate(email, department, position));
        assertEquals(EmployeeRegistrationException.Reason.EMAIL_ALREADY_EXISTS, error.reason());
    }
}
