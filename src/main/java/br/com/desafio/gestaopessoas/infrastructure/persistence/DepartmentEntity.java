package br.com.desafio.gestaopessoas.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "departments")
public class DepartmentEntity {
    @Id UUID id;
    @Column(nullable = false, length = 120) String name;
    @Column(name = "cost_center_code", nullable = false, length = 50) String costCenterCode;
    protected DepartmentEntity() {}
    DepartmentEntity(UUID id, String name, String code) { this.id = id; this.name = name; this.costCenterCode = code; }
}
