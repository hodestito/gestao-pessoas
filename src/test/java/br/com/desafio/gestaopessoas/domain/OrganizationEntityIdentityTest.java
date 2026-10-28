package br.com.desafio.gestaopessoas.domain;

import br.com.desafio.gestaopessoas.domain.department.CostCenterCode;
import br.com.desafio.gestaopessoas.domain.department.Department;
import br.com.desafio.gestaopessoas.domain.position.Position;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationEntityIdentityTest {
    @Test void departmentKeepsIdentityWhenDescriptiveValuesDiffer() {
        UUID id = UUID.randomUUID();
        var original = new Department(id, "RH", new CostCenterCode("CC-01"));
        var changed = new Department(id, "Pessoas", new CostCenterCode("CC-02"));
        assertEquals(original, changed);
        assertEquals(changed, original);
        assertEquals(original.hashCode(), changed.hashCode());
        assertEquals(1, new HashSet<>(List.of(original, changed)).size());
        assertNotEquals(null, original);
        assertNotEquals(original, new Position(id, "RH", "Senior"));
    }

    @Test void identicalDepartmentValuesDoNotReplaceIdentity() {
        var first = new Department(UUID.randomUUID(), "RH", new CostCenterCode("CC-01"));
        var second = new Department(UUID.randomUUID(), "RH", new CostCenterCode("CC-01"));
        assertNotEquals(first, second);
        assertEquals(2, new HashSet<>(List.of(first, second)).size());
    }

    @Test void positionKeepsIdentityWhenDescriptiveValuesDiffer() {
        UUID id = UUID.randomUUID();
        var original = new Position(id, "Engenheiro", "Pleno");
        var changed = new Position(id, "Engenheiro de Software", "Senior");
        assertEquals(original, changed);
        assertEquals(changed, original);
        assertEquals(original.hashCode(), changed.hashCode());
        assertEquals(1, new HashSet<>(List.of(original, changed)).size());
        assertNotEquals(null, original);
        assertNotEquals(original, new Department(id, "RH", new CostCenterCode("CC-01")));
    }

    @Test void identicalPositionValuesDoNotReplaceIdentity() {
        var first = new Position(UUID.randomUUID(), "Engenheiro", "Senior");
        var second = new Position(UUID.randomUUID(), "Engenheiro", "Senior");
        assertNotEquals(first, second);
        assertEquals(2, new HashSet<>(List.of(first, second)).size());
    }
}
