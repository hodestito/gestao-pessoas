package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.EmployeeCreatedEvent;

/** Acrescenta o evento à mesma unidade de trabalho do cadastro. */
public interface EmployeeEventOutbox {
    void append(EmployeeCreatedEvent event);
}
