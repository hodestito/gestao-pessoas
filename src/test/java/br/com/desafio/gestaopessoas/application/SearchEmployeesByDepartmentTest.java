package br.com.desafio.gestaopessoas.application;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SearchEmployeesByDepartmentTest {
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final SearchEmployeesByDepartment search = new SearchEmployeesByDepartment(employees);

    @Test void trimsDepartmentAndUsesStablePaging() {
        when(employees.findByDepartmentName(anyString(), any())).thenReturn(Page.empty());
        search.execute("  RH  ", 1, 10);
        var page = ArgumentCaptor.forClass(Pageable.class);
        verify(employees).findByDepartmentName(eq("RH"), page.capture());
        assertEquals(1, page.getValue().getPageNumber());
        assertEquals(10, page.getValue().getPageSize());
        assertEquals("createdAt: ASC,id: ASC", page.getValue().getSort().toString());
    }

    @Test void rejectsMissingDepartmentAndInvalidPagingBeforeRepository() {
        assertThrows(IllegalArgumentException.class, () -> search.execute(null, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> search.execute(" ", 0, 20));
        assertThrows(IllegalArgumentException.class, () -> search.execute("RH", -1, 20));
        assertThrows(IllegalArgumentException.class, () -> search.execute("RH", 0, 101));
        verifyNoInteractions(employees);
    }
}
