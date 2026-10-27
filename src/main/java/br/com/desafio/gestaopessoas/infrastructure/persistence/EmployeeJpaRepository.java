package br.com.desafio.gestaopessoas.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, UUID> {
    boolean existsByEmail(String email);
}
