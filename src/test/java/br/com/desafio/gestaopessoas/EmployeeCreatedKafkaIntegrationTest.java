package br.com.desafio.gestaopessoas;

import br.com.desafio.gestaopessoas.infrastructure.messaging.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named = "kafka.tests", matches = "true")
@ActiveProfiles("kafka")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.outbox.scheduling-enabled=false")
class EmployeeCreatedKafkaIntegrationTest {
    private static final String TOPIC = "employee-created-test-" + UUID.randomUUID();
    private static final String GROUP = "employee-history-test-" + UUID.randomUUID();

    @DynamicPropertySource static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("app.messaging.topic", () -> TOPIC);
        registry.add("app.messaging.group-id", () -> GROUP);
    }

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired OutboxPublishingTransaction publisher;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired JsonMapper json;
    @MockitoSpyBean EmployeeHistoryWriter history;
    @Value("${spring.kafka.bootstrap-servers}") String bootstrap;
    private final UUID department = UUID.randomUUID();
    private final UUID position = UUID.randomUUID();

    @BeforeEach void setup() {
        jdbc.update("insert into departments(id,name,cost_center_code) values(?,?,?)", department, "Kafka", "test");
        jdbc.update("insert into positions(id,title,level) values(?,?,?)", position, "Engenheiro", "Senior");
    }

    @AfterEach void cleanup() {
        jdbc.update("delete from employee_events where employee_id in (select id from employees where department_id=?)", department);
        jdbc.update("delete from outbox_events where employee_id in (select id from employees where department_id=?)", department);
        jdbc.update("delete from employees where department_id=?", department);
        jdbc.update("delete from positions where id=?", position);
        jdbc.update("delete from departments where id=?", department);
    }

    @AfterAll static void deleteOwnTopic(@Autowired org.springframework.kafka.core.KafkaAdmin kafkaAdmin,
                                        @Autowired org.springframework.kafka.config.KafkaListenerEndpointRegistry listeners) throws Exception {
        listeners.stop();
        try (var admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            admin.deleteTopics(List.of(TOPIC)).all().get(10, TimeUnit.SECONDS);
            admin.deleteConsumerGroups(List.of(GROUP)).all().get(10, TimeUnit.SECONDS);
        }
    }

    @Test void httpRegistrationPublishesConsumesRetriesAndDeduplicatesWithRealKafka() throws Exception {
        String body = """
                {"name":"Teste Kafka","email":"kafka-%s@example.com","departmentId":"%s","positionId":"%s"}
                """.formatted(UUID.randomUUID(), department, position);
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/employees"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), response.body());
        UUID employeeId = UUID.fromString(json.readTree(response.body()).get("id").asText());
        var row = jdbc.queryForMap("select event_id,payload,published_at from outbox_events where employee_id=?", employeeId);
        UUID eventId = UUID.fromString(row.get("event_id").toString());
        String payload = (String) row.get("payload");
        assertNull(row.get("published_at"));
        assertEquals(0, countHistory(eventId));

        // Falha transitória antes do insert: o container deve repetir, sem confirmar o offset.
        doThrow(new DataAccessResourceFailureException("transient test failure")).doCallRealMethod()
                .when(history).record(any(EmployeeCreatedMessage.class), eq(payload));
        assertTrue(publisher.publish(eventId));
        await(() -> countHistory(eventId) == 1);
        verify(history, atLeast(2)).record(any(EmployeeCreatedMessage.class), eq(payload));
        assertNotNull(jdbc.queryForObject("select published_at from outbox_events where event_id=?", java.time.OffsetDateTime.class, eventId));

        // Reenvio do mesmo evento como se o processo tivesse parado antes da marcação.
        var metadata = kafka.send(TOPIC, employeeId.toString(), payload).get(15, TimeUnit.SECONDS).getRecordMetadata();
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", bootstrap))) {
            await(() -> {
                try {
                    var offsets = admin.listConsumerGroupOffsets(GROUP).partitionsToOffsetAndMetadata().get(2, TimeUnit.SECONDS);
                    var committed = offsets.get(new TopicPartition(TOPIC, metadata.partition()));
                    return committed != null && committed.offset() >= metadata.offset() + 1;
                } catch (Exception exception) { return false; }
            });
        }
        assertEquals(1, countHistory(eventId), "Offset da duplicata confirmado, mas histórico continua único");
    }

    private int countHistory(UUID eventId) {
        return jdbc.queryForObject("select count(*) from employee_events where event_id=?", Integer.class, eventId);
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(100);
        }
        fail("Processamento assíncrono não concluiu em 20 segundos");
    }
}
