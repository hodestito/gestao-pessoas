package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Employee;
import java.util.Objects;

/** Estado lido e versão esperada para uma atualização concorrente. */
public record VersionedEmployee(Employee employee, long version) {
    public VersionedEmployee { Objects.requireNonNull(employee, "employee"); }
}
