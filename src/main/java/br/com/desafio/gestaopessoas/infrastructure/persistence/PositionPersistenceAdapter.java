package br.com.desafio.gestaopessoas.infrastructure.persistence;

import br.com.desafio.gestaopessoas.application.PositionRepository;
import br.com.desafio.gestaopessoas.domain.position.Position;
import org.springframework.stereotype.Repository;

@Repository
public class PositionPersistenceAdapter implements PositionRepository {
    private final PositionJpaRepository repository;
    PositionPersistenceAdapter(PositionJpaRepository repository) { this.repository = repository; }
    public Position save(Position position) {
        repository.save(new PositionEntity(position.id(), position.title(), position.level()));
        return position;
    }
}
