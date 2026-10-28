package br.com.desafio.gestaopessoas.domain.employee;

import java.util.UUID;

/** Informações externas ao agregado necessárias para decidir a admissão. */
public interface EmployeeRegistrationRequirements {
    boolean departmentExists(UUID departmentId);
    boolean positionExists(UUID positionId);
    boolean emailAvailable(Email email);
}
