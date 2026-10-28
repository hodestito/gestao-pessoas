package br.com.desafio.gestaopessoas.interfaces.rest;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeExceptionHandlerTest {
    private final EmployeeExceptionHandler handler = new EmployeeExceptionHandler();

    @Test void translatesOnlyKnownConstraintsIncludingNestedMessages() {
        record Case(String constraint, int status, String code) {}
        var cases = List.of(
                new Case("uk_employees_email", 409, "EMAIL_ALREADY_EXISTS"),
                new Case("fk_employees_department", 422, "DEPARTMENT_NOT_FOUND"),
                new Case("fk_employees_position", 422, "POSITION_NOT_FOUND"));
        for (var scenario : cases) {
            var detail = new IllegalArgumentException("violates " + scenario.constraint().toUpperCase(Locale.ROOT));
            var failure = new DataIntegrityViolationException("flush failed", new IllegalStateException(null, detail));
            var response = handler.constraintViolation(failure);
            assertEquals(scenario.status(), response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(scenario.code(), response.getBody().code());
        }
    }

    @Test void unexpectedIntegrityFailureIsPropagatedInsteadOfReportedAsBusinessConflict() {
        var failure = new DataIntegrityViolationException("unexpected_integrity_error");
        assertSame(failure, assertThrows(DataIntegrityViolationException.class,
                () -> handler.constraintViolation(failure)));
    }

    @Test void exhaustedOptimisticConflictHasAnExplicitRetryableHttpContract() {
        var response = handler.concurrentModification(new OptimisticLockingFailureException("stale version"));
        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("CONCURRENT_MODIFICATION", response.getBody().code());
    }
}
