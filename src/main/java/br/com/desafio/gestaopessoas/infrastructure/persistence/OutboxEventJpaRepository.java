package br.com.desafio.gestaopessoas.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Transactional(readOnly = true)
    @Query("""
            select event.eventId from OutboxEventEntity event
            where event.publishedAt is null and event.availableAt <= :now
            order by event.occurredAt, event.eventId
            """)
    List<UUID> findAvailableIds(@Param("now") Instant now, Pageable limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event from OutboxEventEntity event
            where event.eventId = :id and event.publishedAt is null and event.availableAt <= :now
            """)
    Optional<OutboxEventEntity> findPendingForUpdate(@Param("id") UUID id, @Param("now") Instant now);
}
