package br.com.desafio.gestaopessoas.infrastructure.config;

import br.com.desafio.gestaopessoas.application.DeactivateEmployee;
import br.com.desafio.gestaopessoas.application.DeactivationResult;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionalEmployeeDeactivation {
    private final DeactivateEmployee useCase;
    public TransactionalEmployeeDeactivation(DeactivateEmployee useCase) { this.useCase = useCase; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeactivationResult execute(UUID id) { return useCase.execute(id); }
}
