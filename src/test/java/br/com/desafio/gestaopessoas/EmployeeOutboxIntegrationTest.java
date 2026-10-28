package br.com.desafio.gestaopessoas;

import br.com.desafio.gestaopessoas.application.EmployeeRegistration;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeCreatedEvent;
import br.com.desafio.gestaopessoas.infrastructure.messaging.*;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmployeeOutboxIntegrationTest {
    @Autowired EmployeeRegistration registration;
    @Autowired JpaEmployeeEventOutbox outbox;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;
    @Autowired EmployeeHistoryWriter history;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Autowired PlatformTransactionManager transactions;
    private final UUID department = UUID.randomUUID();
    private final UUID position = UUID.randomUUID();
    private final String email = "outbox-" + UUID.randomUUID() + "@example.com";

    @BeforeEach void setup() {
        jdbc.update("insert into departments(id,name,cost_center_code) values(?,?,?)", department, "Outbox", "test");
        jdbc.update("insert into positions(id,title,level) values(?,?,?)", position, "Engenheiro", "Senior");
    }

    @AfterEach void cleanup() {
        jdbc.update("delete from employee_events where employee_id in (select id from employees where department_id=?)", department);
        jdbc.update("delete from outbox_events where employee_id in (select id from employees where department_id=?)", department);
        jdbc.update("delete from employees where department_id=?", department);
        jdbc.update("delete from positions where id=?", position);
        jdbc.update("delete from departments where id=?", department);
    }

    @Test void commitsEmployeeAndEventWithStableIdAndWithoutPersonalData() {
        var employee = registration.execute("Nome privado", email, department, position);
        var row = jdbc.queryForMap("select * from outbox_events where employee_id=?", employee.id());
        var event = json.readValue((String) row.get("payload"), EmployeeCreatedMessage.class);
        assertEquals(employee.id(), event.employeeId());
        assertEquals(employee.createdAt(), event.occurredAt());
        assertEquals(row.get("event_id").toString(), event.eventId().toString());
        assertNull(row.get("published_at"));
        assertFalse(((String) row.get("payload")).contains(email));
        assertFalse(((String) row.get("payload")).contains("Nome privado"));
        assertEquals(0, jdbc.queryForObject("select count(*) from employee_events where employee_id=?", Integer.class, employee.id()));
    }

    @Test void databaseFailureAfterOutboxInsertRollsBackEmployeeAndOutboxTogether() {
        UUID[] created = new UUID[1];
        assertThrows(DataIntegrityViolationException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var employee = registration.execute("Teste", email, department, position);
            created[0] = employee.id();
            UUID eventId = jdbc.queryForObject("select event_id from outbox_events where employee_id=?", UUID.class, employee.id());
            // Força falha real no banco dentro da mesma transação, após os dois inserts.
            entityManager.clear();
            outbox.append(new EmployeeCreatedEvent(eventId, employee.id(), employee.createdAt()));
        }));
        assertNotNull(created[0]);
        assertEquals(0, jdbc.queryForObject("select count(*) from employees where id=?", Integer.class, created[0]));
        assertEquals(0, jdbc.queryForObject("select count(*) from outbox_events where employee_id=?", Integer.class, created[0]));
    }

    @Test void outboxRequiresAnExistingTransaction() {
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                () -> outbox.append(new EmployeeCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), java.time.Instant.now())));
    }

    @Test void repeatedDeliveryCommitsHistoryOnceAndRejectsWrongKey() {
        var employee = registration.execute("Teste", email, department, position);
        String payload = jdbc.queryForObject("select payload from outbox_events where employee_id=?", String.class, employee.id());
        var consumer = new EmployeeCreatedKafkaConsumer(json, new EmployeeEventHistoryHandler(history));
        var record = new ConsumerRecord<>("test", 0, 0, employee.id().toString(), payload);
        consumer.consume(record);
        consumer.consume(record);
        var original = json.readValue(payload, EmployeeCreatedMessage.class);
        var changed = new EmployeeCreatedMessage(1, original.eventType(), original.eventId(),
                original.employeeId(), original.occurredAt().plusSeconds(10));
        consumer.consume(new ConsumerRecord<>("test", 0, 2, employee.id().toString(), json.writeValueAsString(changed)));
        assertEquals(1, jdbc.queryForObject("select count(*) from employee_events where employee_id=?", Integer.class, employee.id()));
        assertEquals(payload, jdbc.queryForObject("select payload from employee_events where employee_id=?", String.class, employee.id()),
                "Um evento com id existente não pode sobrescrever o histórico via merge");
        assertThrows(IllegalArgumentException.class, () -> consumer.consume(new ConsumerRecord<>("test", 0, 1, "wrong", payload)));
    }
}
