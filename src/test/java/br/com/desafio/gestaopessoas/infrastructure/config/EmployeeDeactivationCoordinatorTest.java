package br.com.desafio.gestaopessoas.infrastructure.config;

import br.com.desafio.gestaopessoas.application.DeactivationResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeDeactivationCoordinatorTest {
    private final TransactionalEmployeeDeactivation transaction = mock(TransactionalEmployeeDeactivation.class);
    private final EmployeeDeactivationCoordinator coordinator = new EmployeeDeactivationCoordinator(transaction);
    private final UUID id = UUID.randomUUID();

    @Test void rereadsOnceAfterOptimisticConflict() {
        when(transaction.execute(id)).thenThrow(new OptimisticLockingFailureException("conflict"))
                .thenReturn(DeactivationResult.ALREADY_INACTIVE);
        assertEquals(DeactivationResult.ALREADY_INACTIVE, coordinator.execute(id));
        verify(transaction, times(2)).execute(id);
    }

    @Test void propagatesSecondConflictWithoutInfiniteRetry() {
        when(transaction.execute(id)).thenThrow(new OptimisticLockingFailureException("conflict"));
        assertThrows(OptimisticLockingFailureException.class, () -> coordinator.execute(id));
        verify(transaction, times(2)).execute(id);
    }
}
