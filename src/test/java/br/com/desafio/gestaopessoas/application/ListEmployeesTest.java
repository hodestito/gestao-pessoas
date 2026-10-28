package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import br.com.desafio.gestaopessoas.domain.employee.Employee;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ListEmployeesTest {
    private final EmployeeRepository repository = mock(EmployeeRepository.class);
    private final ListEmployees useCase = new ListEmployees(repository);

    @Test void passesFiltersAndOwnPaginationToRepository() {
        var result = new PageResult<Employee>(List.of(), 1, 20, 0);
        when(repository.findAll(anyString(), eq(EmployeeStatus.INACTIVE), any())).thenReturn(result);
        assertSame(result, useCase.execute("  Hugo  ", EmployeeStatus.INACTIVE, 1, 20));
        var pageable = ArgumentCaptor.forClass(EmployeePagination.class);
        verify(repository).findAll(eq("Hugo"), eq(EmployeeStatus.INACTIVE), pageable.capture());
        assertEquals(new EmployeePagination(1, 20), pageable.getValue());
    }

    @Test void rejectsPageAndSizeOutsideContractBeforeQuerying() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, -1, 20));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, 0, 101));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, Integer.MAX_VALUE, 100));
        verifyNoInteractions(repository);
    }

    @Test void treatsBlankNameAsNoFilter() {
        when(repository.findAll(isNull(), isNull(), any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0));
        useCase.execute("  ", null, 0, 20);
        verify(repository).findAll(isNull(), isNull(), any());
    }
}
