package br.com.desafio.gestaopessoas.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "positions")
public class PositionEntity {
    @Id UUID id;
    @Column(nullable = false, length = 120) String title;
    @Column(nullable = false, length = 50) String level;
    protected PositionEntity() {}
    PositionEntity(UUID id, String title, String level) { this.id = id; this.title = title; this.level = level; }
}
