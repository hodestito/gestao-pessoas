package br.com.desafio.gestaopessoas.domain.department;

import br.com.desafio.gestaopessoas.domain.RequiredText;
import java.util.Objects;
import java.util.UUID;

public final class Department {
    private final UUID id;
    private final String name;
    private final CostCenterCode costCenterCode;

    public Department(UUID id, String name, CostCenterCode costCenterCode) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = RequiredText.normalize(name, "name", 120);
        this.costCenterCode = Objects.requireNonNull(costCenterCode, "costCenterCode");
    }
    public static Department create(String name, String code) {
        return new Department(UUID.randomUUID(), name, new CostCenterCode(code));
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public CostCenterCode costCenterCode() { return costCenterCode; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof Department department && id.equals(department.id);
    }

    @Override public int hashCode() { return id.hashCode(); }
}
