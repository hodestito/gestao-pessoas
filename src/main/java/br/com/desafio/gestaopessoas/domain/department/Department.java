package br.com.desafio.gestaopessoas.domain.department;

import br.com.desafio.gestaopessoas.domain.RequiredText;
import java.util.Objects;
import java.util.UUID;

public record Department(UUID id, String name, CostCenterCode costCenterCode) {
    public Department {
        Objects.requireNonNull(id, "id");
        name = RequiredText.normalize(name, "name", 120);
        Objects.requireNonNull(costCenterCode, "costCenterCode");
    }
    public static Department create(String name, String code) {
        return new Department(UUID.randomUUID(), name, new CostCenterCode(code));
    }
}
