package br.com.desafio.gestaopessoas.infrastructure.messaging;

import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeHistoryWriter {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public EmployeeHistoryWriter(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(EmployeeCreatedMessage event, String payload) {
        jdbc.update("""
                INSERT INTO employee_events
                    (event_id, employee_id, event_type, schema_version, occurred_at, processed_at, payload)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, event.eventId(), event.employeeId(), event.eventType(), event.schemaVersion(),
                event.occurredAt().atOffset(ZoneOffset.UTC), clock.instant().atOffset(ZoneOffset.UTC), payload);
    }
}
