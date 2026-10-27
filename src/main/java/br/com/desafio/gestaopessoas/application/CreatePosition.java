package br.com.desafio.gestaopessoas.application;

import br.com.desafio.gestaopessoas.domain.position.Position;

public class CreatePosition {
    private final PositionRepository repository;
    public CreatePosition(PositionRepository repository) { this.repository = repository; }
    public Position execute(String title, String level) { return repository.save(Position.create(title, level)); }
}
