package br.com.desafio.gestaopessoas.domain.employee;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailTest {
    @Test void normalizesAndComparesByValue() {
        assertEquals(new Email("hugo@example.com"), new Email("  HUGO@EXAMPLE.COM  "));
        assertEquals("hugo@example.com", new Email("  HUGO@EXAMPLE.COM  ").value());
    }

    @Test void rejectsMissingAndInvalidAddresses() {
        for (String value : new String[] {"", " ", "hugo", "@example.com", "hugo@", "hugo@@example.com", "hugo@-example.com", "hugo@exam ple.com", "hugo..x@example.com"}) {
            assertThrows(IllegalArgumentException.class, () -> new Email(value), value);
        }
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
        assertThrows(IllegalArgumentException.class, () -> new Email("a".repeat(250) + "@example.com"));
    }
}
