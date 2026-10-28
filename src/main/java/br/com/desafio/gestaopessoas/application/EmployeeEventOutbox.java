package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.EmployeeCreatedEvent;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatusChangedEvent;

/** Acrescenta o evento à mesma unidade de trabalho da mudança do empregado. */
public interface EmployeeEventOutbox {
    void append(EmployeeCreatedEvent event);
    void append(EmployeeStatusChangedEvent event);
}
