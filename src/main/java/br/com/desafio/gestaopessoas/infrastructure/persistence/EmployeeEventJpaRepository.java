package br.com.desafio.gestaopessoas.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeEventJpaRepository extends JpaRepository<EmployeeEventEntity, UUID> {}
