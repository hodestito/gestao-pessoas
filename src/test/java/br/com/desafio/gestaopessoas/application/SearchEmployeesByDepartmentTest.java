package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.Employee;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SearchEmployeesByDepartmentTest {
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final SearchEmployeesByDepartment search = new SearchEmployeesByDepartment(employees);

    @Test void trimsDepartmentAndUsesOwnPaging() {
        var result = new PageResult<Employee>(List.of(), 1, 10, 0);
        when(employees.findByDepartmentName(anyString(), any())).thenReturn(result);
        assertSame(result, search.execute("  RH  ", 1, 10));
        var page = ArgumentCaptor.forClass(EmployeePagination.class);
        verify(employees).findByDepartmentName(eq("RH"), page.capture());
        assertEquals(new EmployeePagination(1, 10), page.getValue());
    }

    @Test void rejectsMissingDepartmentAndInvalidPagingBeforeRepository() {
        assertThrows(IllegalArgumentException.class, () -> search.execute(null, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> search.execute(" ", 0, 20));
        assertThrows(IllegalArgumentException.class, () -> search.execute("RH", -1, 20));
        assertThrows(IllegalArgumentException.class, () -> search.execute("RH", 0, 101));
        verifyNoInteractions(employees);
    }
}
