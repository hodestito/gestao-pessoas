package br.com.desafio.gestaopessoas.infrastructure.persistence;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
interface PositionJpaRepository extends JpaRepository<PositionEntity, UUID> {}
