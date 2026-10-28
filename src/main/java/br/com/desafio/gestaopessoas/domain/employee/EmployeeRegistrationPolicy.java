package br.com.desafio.gestaopessoas.domain.employee;

import java.util.Objects;
import java.util.UUID;
import static br.com.desafio.gestaopessoas.domain.employee.EmployeeRegistrationException.Reason.*;

/** Serviço de domínio: regras de admissão que envolvem outros registros. */
public class EmployeeRegistrationPolicy {
    private final EmployeeRegistrationRequirements requirements;

    public EmployeeRegistrationPolicy(EmployeeRegistrationRequirements requirements) {
        this.requirements = Objects.requireNonNull(requirements, "requirements");
    }

    public void validate(Email email, UUID departmentId, UUID positionId) {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(departmentId, "departmentId");
        Objects.requireNonNull(positionId, "positionId");
        if (!requirements.departmentExists(departmentId)) {
            throw new EmployeeRegistrationException(DEPARTMENT_NOT_FOUND);
        }
        if (!requirements.positionExists(positionId)) {
            throw new EmployeeRegistrationException(POSITION_NOT_FOUND);
        }
        if (!requirements.emailAvailable(email)) {
            throw new EmployeeRegistrationException(EMAIL_ALREADY_EXISTS);
        }
    }
}
