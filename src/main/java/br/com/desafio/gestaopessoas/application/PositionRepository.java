package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.position.Position;
import java.util.UUID;

public interface PositionRepository {
    Position save(Position position);
    boolean existsById(UUID id);
}
