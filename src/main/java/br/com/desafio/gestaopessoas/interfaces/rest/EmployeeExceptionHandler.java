package br.com.desafio.gestaopessoas.interfaces.rest;

import br.com.desafio.gestaopessoas.domain.employee.EmployeeRegistrationException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EmployeeController.class)
public class EmployeeExceptionHandler {
    record ErrorResponse(String code, String message) {}

    @ExceptionHandler(EmployeeRegistrationException.class)
    ResponseEntity<ErrorResponse> registrationError(EmployeeRegistrationException exception) {
        return switch (exception.reason()) {
            case EMAIL_ALREADY_EXISTS -> response(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "E-mail já cadastrado");
            case DEPARTMENT_NOT_FOUND -> response(HttpStatus.UNPROCESSABLE_ENTITY, "DEPARTMENT_NOT_FOUND", "Departamento inexistente");
            case POSITION_NOT_FOUND -> response(HttpStatus.UNPROCESSABLE_ENTITY, "POSITION_NOT_FOUND", "Cargo inexistente");
        };
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalidDomainValue(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_INPUT", exception.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> concurrentModification(OptimisticLockingFailureException exception) {
        return response(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Colaborador alterado concorrentemente; tente novamente");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> constraintViolation(DataIntegrityViolationException exception) {
        // Restringe a tradução às constraints conhecidas; falhas inesperadas não são mascaradas.
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message == null) continue;
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("uk_employees_email")) return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "E-mail já cadastrado");
            if (normalized.contains("fk_employees_department")) return response(HttpStatus.UNPROCESSABLE_ENTITY, "DEPARTMENT_NOT_FOUND", "Departamento inexistente");
            if (normalized.contains("fk_employees_position")) return response(HttpStatus.UNPROCESSABLE_ENTITY, "POSITION_NOT_FOUND", "Cargo inexistente");
        }
        throw exception;
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}
