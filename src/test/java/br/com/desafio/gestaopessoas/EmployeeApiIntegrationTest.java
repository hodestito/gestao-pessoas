package br.com.desafio.gestaopessoas;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmployeeApiIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final UUID department = UUID.randomUUID();
    private final UUID position = UUID.randomUUID();
    private final String email = "test-" + UUID.randomUUID() + "@example.com";

    @BeforeEach void setup() {
        jdbc.update("insert into departments(id, name, cost_center_code) values (?, ?, ?)", department, "RH", "test");
        jdbc.update("insert into positions(id, title, level) values (?, ?, ?)", position, "Engenheiro", "Senior");
    }

    @AfterEach void cleanup() {
        jdbc.update("delete from outbox_events where employee_id in (select id from employees where email = ?)", email);
        jdbc.update("delete from employees where email = ?", email);
        jdbc.update("delete from positions where id = ?", position);
        jdbc.update("delete from departments where id = ?", department);
    }

    private HttpResponse<String> post(String address, UUID departmentId, UUID positionId) throws Exception {
        String body = "{\"name\":\"Hugo\",\"email\":\"" + address + "\",\"departmentId\":\"" + departmentId + "\",\"positionId\":\"" + positionId + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/employees"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test void createsEmployeeAndRejectsNormalizedDuplicate() throws Exception {
        var response = post(" " + email.toUpperCase(java.util.Locale.ROOT) + " ", department, position);
        assertEquals(201, response.statusCode());
        UUID id = UUID.fromString(response.headers().firstValue("Location").orElseThrow().replace("/employees/", ""));
        assertEquals("ACTIVE", jdbc.queryForObject("select status from employees where id = ?", String.class, id));
        assertEquals(email, jdbc.queryForObject("select email from employees where id = ?", String.class, id));
        assertTrue(response.body().contains("\"createdAt\""));
        assertEquals(1, jdbc.queryForObject("select count(*) from outbox_events where employee_id = ?", Integer.class, id));
        var duplicate = post(email, department, position);
        assertEquals(409, duplicate.statusCode());
        assertTrue(duplicate.body().contains("EMAIL_ALREADY_EXISTS"));
    }

    @Test void rejectsMissingReferencesAndInvalidEmail() throws Exception {
        assertEquals(422, post(email, UUID.randomUUID(), position).statusCode());
        assertEquals(422, post(email, department, UUID.randomUUID()).statusCode());
        assertEquals(400, post("invalid", department, position).statusCode());
        assertEquals(0, jdbc.queryForObject("select count(*) from employees where email = ?", Integer.class, email));
    }

    @Test void concurrentRequestsAllowOnlyOneEmployee() throws Exception {
        var first = CompletableFuture.supplyAsync(() -> sendUnchecked());
        var second = CompletableFuture.supplyAsync(() -> sendUnchecked());
        var statuses = java.util.List.of(first.get(), second.get());
        assertTrue(statuses.contains(201), statuses.toString());
        assertTrue(statuses.contains(409), statuses.toString());
        assertEquals(1, jdbc.queryForObject("select count(*) from employees where email = ?", Integer.class, email));
    }

    @Test void databaseRejectsDuplicateEmailEvenForInactiveEmployee() throws Exception {
        assertEquals(201, post(email, department, position).statusCode());
        jdbc.update("update employees set status = 'INACTIVE' where email = ?", email);
        assertEquals(409, post(email, department, position).statusCode());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into employees(id,name,email,department_id,position_id,created_at,status) values(?,?,?,?,?,current_timestamp,'ACTIVE')",
                UUID.randomUUID(), "Outro", email, department, position));
    }

    private int sendUnchecked() {
        try { return post(email, department, position).statusCode(); }
        catch (Exception exception) { throw new RuntimeException(exception); }
    }
}
