package br.com.desafio.gestaopessoas.domain.employee;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeStatusChangedEventTest {
    @Test void requiresAnActualChangeAndCompleteIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new EmployeeStatusChangedEvent(UUID.randomUUID(),
                UUID.randomUUID(), Instant.EPOCH, EmployeeStatus.ACTIVE, EmployeeStatus.ACTIVE));
        assertThrows(NullPointerException.class, () -> new EmployeeStatusChangedEvent(null, UUID.randomUUID(),
                Instant.EPOCH, EmployeeStatus.ACTIVE, EmployeeStatus.INACTIVE));
        assertThrows(NullPointerException.class, () -> new EmployeeStatusChangedEvent(UUID.randomUUID(), UUID.randomUUID(),
                Instant.EPOCH, null, EmployeeStatus.INACTIVE));
    }
}
