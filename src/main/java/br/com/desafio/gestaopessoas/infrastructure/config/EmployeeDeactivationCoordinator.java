package br.com.desafio.gestaopessoas.infrastructure.config;

import br.com.desafio.gestaopessoas.application.DeactivationResult;
import br.com.desafio.gestaopessoas.application.EmployeeDeactivation;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class EmployeeDeactivationCoordinator implements EmployeeDeactivation {
    private final TransactionalEmployeeDeactivation transaction;
    public EmployeeDeactivationCoordinator(TransactionalEmployeeDeactivation transaction) { this.transaction = transaction; }

    public DeactivationResult execute(UUID id) {
        try {
            return transaction.execute(id);
        } catch (OptimisticLockingFailureException conflict) {
            // A primeira transação já terminou; a nova tentativa relê o estado.
            return transaction.execute(id);
        }
    }
}
