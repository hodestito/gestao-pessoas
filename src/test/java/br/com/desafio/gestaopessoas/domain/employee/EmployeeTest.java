package br.com.desafio.gestaopessoas.domain.employee;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeTest {
    private static final UUID ID = UUID.fromString("8f9eb9f2-ab74-4dd7-a77d-458a127f96bc");
    private static final UUID DEPARTMENT_ID = UUID.fromString("533da324-440d-4087-ad59-fe6cfe135b73");
    private static final UUID POSITION_ID = UUID.fromString("aaf728d6-e33e-457b-8697-15da8ddf3111");
    private static final Instant CREATED_AT = Instant.parse("2026-10-27T12:00:00Z");

    private Employee create(UUID id, String name) {
        return Employee.create(id, name, new Email("hugo@example.com"), DEPARTMENT_ID, POSITION_ID, CREATED_AT);
    }

    @Test void createsActiveEmployeeWithSuppliedTimeAndReferences() {
        Employee employee = create(ID, "  Hugo  ");
        assertEquals("Hugo", employee.name());
        assertEquals(EmployeeStatus.ACTIVE, employee.status());
        assertEquals(CREATED_AT, employee.createdAt());
        assertEquals(DEPARTMENT_ID, employee.departmentId());
        assertEquals(POSITION_ID, employee.positionId());
    }

    @Test void rejectsInvalidNameAndMissingReferences() {
        assertThrows(IllegalArgumentException.class, () -> create(ID, " "));
        assertThrows(IllegalArgumentException.class, () -> create(ID, "x".repeat(121)));
        assertEquals(120, create(ID, "x".repeat(120)).name().length());
        assertThrows(NullPointerException.class, () -> Employee.create(null, "Hugo", new Email("hugo@example.com"), DEPARTMENT_ID, POSITION_ID, CREATED_AT));
        assertThrows(NullPointerException.class, () -> Employee.create(ID, "Hugo", null, DEPARTMENT_ID, POSITION_ID, CREATED_AT));
        assertThrows(NullPointerException.class, () -> Employee.create(ID, "Hugo", new Email("hugo@example.com"), null, POSITION_ID, CREATED_AT));
        assertThrows(NullPointerException.class, () -> Employee.create(ID, "Hugo", new Email("hugo@example.com"), DEPARTMENT_ID, null, CREATED_AT));
        assertThrows(NullPointerException.class, () -> Employee.create(ID, "Hugo", new Email("hugo@example.com"), DEPARTMENT_ID, POSITION_ID, null));
    }

    @Test void inactivationChangesStateOnlyOnce() {
        Employee employee = create(ID, "Hugo");
        assertTrue(employee.deactivate());
        assertEquals(EmployeeStatus.INACTIVE, employee.status());
        assertFalse(employee.deactivate());
        assertEquals(EmployeeStatus.INACTIVE, employee.status());
    }

    @Test void equalityAndHashCodeUseIdentityEvenAfterStateChanges() {
        Employee first = create(ID, "Hugo");
        Employee sameIdentity = create(ID, "Outro nome");
        Employee differentIdentity = create(UUID.randomUUID(), "Hugo");
        int originalHash = first.hashCode();
        first.deactivate();
        assertEquals(first, sameIdentity);
        assertEquals(originalHash, first.hashCode());
        assertEquals(first.hashCode(), sameIdentity.hashCode());
        assertNotEquals(first, differentIdentity);
    }
}
