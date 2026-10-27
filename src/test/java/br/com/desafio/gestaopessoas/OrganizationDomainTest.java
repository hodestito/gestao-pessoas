package br.com.desafio.gestaopessoas;

import br.com.desafio.gestaopessoas.application.CreateDepartment;
import br.com.desafio.gestaopessoas.application.CreatePosition;
import br.com.desafio.gestaopessoas.domain.department.Department;
import br.com.desafio.gestaopessoas.domain.position.Position;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationDomainTest {
    @Test void createsAndNormalizesDepartment() {
        var department = new CreateDepartment(d -> d).execute("  RH  ", " CC-01 ");
        assertNotNull(department.id());
        assertEquals("RH", department.name());
        assertEquals("CC-01", department.costCenterCode().value());
    }
    @Test void createsAndNormalizesPosition() {
        var position = new CreatePosition(p -> p).execute(" Engenheiro ", " Sênior ");
        assertNotNull(position.id());
        assertEquals("Engenheiro", position.title());
        assertEquals("Sênior", position.level());
    }
    @Test void rejectsInvalidValuesBeforeSaving() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var useCase = new CreateDepartment(d -> { calls.incrementAndGet(); return d; });
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(" ", "1"));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("RH", " "));
        assertEquals(0, calls.get());
        assertThrows(IllegalArgumentException.class, () -> Position.create("x".repeat(121), "Senior"));
        assertThrows(IllegalArgumentException.class, () -> Department.create(null, "1"));
    }
}
