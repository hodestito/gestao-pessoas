package br.com.desafio.gestaopessoas.infrastructure.messaging;

import br.com.desafio.gestaopessoas.application.EmployeeEventOutbox;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeCreatedEvent;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatusChangedEvent;
import br.com.desafio.gestaopessoas.infrastructure.persistence.OutboxEventEntity;
import br.com.desafio.gestaopessoas.infrastructure.persistence.OutboxEventJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JpaEmployeeEventOutbox implements EmployeeEventOutbox {
    private final OutboxEventJpaRepository repository;
    private final JsonMapper json;

    public JpaEmployeeEventOutbox(OutboxEventJpaRepository repository, JsonMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(EmployeeCreatedEvent event) {
        save(EmployeeCreatedMessage.from(event));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(EmployeeStatusChangedEvent event) {
        save(EmployeeStatusChangedMessage.from(event));
    }

    private void save(EmployeeEventMessage message) {
        repository.saveAndFlush(new OutboxEventEntity(message, json.writeValueAsString(message)));
    }
}
