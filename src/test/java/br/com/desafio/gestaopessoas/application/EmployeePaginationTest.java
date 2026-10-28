package br.com.desafio.gestaopessoas.application;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmployeePaginationTest {
    @Test void validatesDirectConstructionAndPreservesTheSupportedOffsetBoundary() {
        assertEquals(new EmployeePagination(0, 100), EmployeePagination.create(0, 100));
        assertDoesNotThrow(() -> new EmployeePagination(Integer.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> new EmployeePagination(Integer.MAX_VALUE, 2));
        assertThrows(IllegalArgumentException.class, () -> new EmployeePagination(-1, 20));
        assertThrows(IllegalArgumentException.class, () -> new EmployeePagination(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new EmployeePagination(0, 101));
    }
}
