package br.com.desafio.gestaopessoas.application;

import java.util.Objects;
import java.util.UUID;

public class DeactivateEmployee {
    private final EmployeeRepository employees;

    public DeactivateEmployee(EmployeeRepository employees) { this.employees = employees; }

    public DeactivationResult execute(UUID id) {
        Objects.requireNonNull(id, "id");
        var snapshot = employees.findVersionedById(id);
        if (snapshot.isEmpty()) return DeactivationResult.NOT_FOUND;
        var employee = snapshot.get();
        if (!employee.employee().deactivate()) return DeactivationResult.ALREADY_INACTIVE;
        employees.saveVersioned(employee);
        return DeactivationResult.DEACTIVATED;
    }
}
