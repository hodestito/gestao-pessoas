package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatusChangedEvent;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class DeactivateEmployee {
    private final EmployeeRepository employees;
    private final EmployeeEventOutbox outbox;
    private final Clock clock;
    private final Supplier<UUID> eventIds;

    public DeactivateEmployee(EmployeeRepository employees, EmployeeEventOutbox outbox, Clock clock, Supplier<UUID> eventIds) {
        this.employees = employees;
        this.outbox = outbox;
        this.clock = clock;
        this.eventIds = eventIds;
    }

    public DeactivationResult execute(UUID id) {
        Objects.requireNonNull(id, "id");
        var snapshot = employees.findVersionedById(id);
        if (snapshot.isEmpty()) return DeactivationResult.NOT_FOUND;
        var employee = snapshot.get();
        var previousStatus = employee.employee().status();
        if (!employee.employee().deactivate()) return DeactivationResult.ALREADY_INACTIVE;
        employees.saveVersioned(employee);
        outbox.append(new EmployeeStatusChangedEvent(eventIds.get(), id, clock.instant(),
                previousStatus, employee.employee().status()));
        return DeactivationResult.DEACTIVATED;
    }
}
