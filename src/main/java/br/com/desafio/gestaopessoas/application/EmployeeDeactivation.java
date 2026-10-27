package br.com.desafio.gestaopessoas.application;

import java.util.UUID;

public interface EmployeeDeactivation {
    DeactivationResult execute(UUID id);
}
