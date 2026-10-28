package br.com.desafio.gestaopessoas.application;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {
    @Test void calculatesEmptyExactAndPartialPageCountsWithoutOverflow() {
        assertEquals(0, new PageResult<>(List.of(), 0, 20, 0).totalPages());
        assertEquals(2, new PageResult<>(List.of(), 0, 2, 4).totalPages());
        assertEquals(2, new PageResult<>(List.of(), 0, 2, 3).totalPages());
        assertEquals(Integer.MAX_VALUE, new PageResult<>(List.of(), 0, 100, Long.MAX_VALUE).totalPages());
    }

    @Test void takesAnImmutableCopyOfTheContent() {
        var source = new ArrayList<>(List.of("first"));
        var result = new PageResult<>(source, 0, 2, 1);
        source.add("second");
        assertEquals(List.of("first"), result.content());
        assertThrows(UnsupportedOperationException.class, () -> result.content().add("third"));
    }

    @Test void permitsAnEmptyPageBeyondTheLastPage() {
        var result = new PageResult<>(List.of(), 9, 2, 3);
        assertEquals(9, result.page());
        assertEquals(2, result.totalPages());
        assertTrue(result.content().isEmpty());
    }

    @Test void rejectsInvalidMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(List.of(), -1, 20, 0));
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(List.of(), 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(List.of(), 0, 20, -1));
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(List.of("one", "two"), 0, 1, 2));
    }
}
