package br.com.desafio.gestaopessoas.infrastructure.messaging;

import br.com.desafio.gestaopessoas.application.EmployeeEventOutbox;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeCreatedEvent;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JdbcEmployeeEventOutbox implements EmployeeEventOutbox {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public JdbcEmployeeEventOutbox(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(EmployeeCreatedEvent event) {
        EmployeeCreatedMessage message = EmployeeCreatedMessage.from(event);
        jdbc.update("""
                INSERT INTO outbox_events
                    (event_id, employee_id, event_type, schema_version, occurred_at, payload, available_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, event.eventId(), event.employeeId(), message.eventType(), message.schemaVersion(),
                event.occurredAt().atOffset(ZoneOffset.UTC), json.writeValueAsString(message),
                event.occurredAt().atOffset(ZoneOffset.UTC));
    }
}
