package br.com.desafio.gestaopessoas.infrastructure.messaging;

import br.com.desafio.gestaopessoas.infrastructure.persistence.EmployeeEventEntity;
import br.com.desafio.gestaopessoas.infrastructure.persistence.EmployeeEventJpaRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeHistoryWriter {
    private final EmployeeEventJpaRepository repository;
    private final Clock clock;

    public EmployeeHistoryWriter(EmployeeEventJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(EmployeeCreatedMessage event, String payload) {
        repository.saveAndFlush(new EmployeeEventEntity(event, clock.instant(), payload));
    }

    public boolean contains(UUID eventId) { return repository.existsById(eventId); }
}
