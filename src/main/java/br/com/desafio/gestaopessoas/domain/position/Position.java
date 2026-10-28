package br.com.desafio.gestaopessoas.domain.position;

import br.com.desafio.gestaopessoas.domain.RequiredText;
import java.util.Objects;
import java.util.UUID;

public final class Position {
    private final UUID id;
    private final String title;
    private final String level;

    public Position(UUID id, String title, String level) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = RequiredText.normalize(title, "title", 120);
        this.level = RequiredText.normalize(level, "level", 50);
    }
    public static Position create(String title, String level) {
        return new Position(UUID.randomUUID(), title, level);
    }

    public UUID id() { return id; }
    public String title() { return title; }
    public String level() { return level; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof Position position && id.equals(position.id);
    }

    @Override public int hashCode() { return id.hashCode(); }
}
