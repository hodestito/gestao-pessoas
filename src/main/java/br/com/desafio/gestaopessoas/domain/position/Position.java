package br.com.desafio.gestaopessoas.domain.position;

import br.com.desafio.gestaopessoas.domain.RequiredText;
import java.util.Objects;
import java.util.UUID;

public record Position(UUID id, String title, String level) {
    public Position {
        Objects.requireNonNull(id, "id");
        title = RequiredText.normalize(title, "title", 120);
        level = RequiredText.normalize(level, "level", 50);
    }
    public static Position create(String title, String level) {
        return new Position(UUID.randomUUID(), title, level);
    }
}
