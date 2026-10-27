package br.com.desafio.gestaopessoas.application;

public class EmployeeRegistrationException extends RuntimeException {
    public enum Reason { DEPARTMENT_NOT_FOUND, POSITION_NOT_FOUND, EMAIL_ALREADY_EXISTS }
    private final Reason reason;

    public EmployeeRegistrationException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
