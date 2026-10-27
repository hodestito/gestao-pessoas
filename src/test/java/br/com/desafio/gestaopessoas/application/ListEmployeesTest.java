package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ListEmployeesTest {
    private final EmployeeRepository repository = mock(EmployeeRepository.class);
    private final ListEmployees useCase = new ListEmployees(repository);

    @Test void passesFiltersAndStablePaginationToRepository() {
        when(repository.findAll(anyString(), eq(EmployeeStatus.INACTIVE), any())).thenReturn(Page.empty());
        useCase.execute("  Hugo  ", EmployeeStatus.INACTIVE, 1, 20);
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(eq("Hugo"), eq(EmployeeStatus.INACTIVE), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(20, pageable.getValue().getPageSize());
        assertEquals("createdAt: ASC,id: ASC", pageable.getValue().getSort().toString());
    }

    @Test void rejectsPageAndSizeOutsideContractBeforeQuerying() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, -1, 20));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, 0, 101));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, Integer.MAX_VALUE, 100));
        verifyNoInteractions(repository);
    }

    @Test void treatsBlankNameAsNoFilter() {
        when(repository.findAll(isNull(), isNull(), any())).thenReturn(Page.empty());
        useCase.execute("  ", null, 0, 20);
        verify(repository).findAll(isNull(), isNull(), any());
    }
}
