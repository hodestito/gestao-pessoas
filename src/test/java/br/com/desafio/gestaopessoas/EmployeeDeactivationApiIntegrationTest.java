package br.com.desafio.gestaopessoas;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import br.com.desafio.gestaopessoas.support.WithIntegrationContainers;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@WithIntegrationContainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmployeeDeactivationApiIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final UUID department = UUID.randomUUID();
    private final UUID position = UUID.randomUUID();
    private final UUID employee = UUID.randomUUID();

    @BeforeEach void setup() {
        jdbc.update("insert into departments(id,name,cost_center_code) values(?,?,?)", department, "RH", "test");
        jdbc.update("insert into positions(id,title,level) values(?,?,?)", position, "Dev", "Senior");
        jdbc.update("insert into employees(id,name,email,department_id,position_id,created_at,status) values(?,?,?,?,?,current_timestamp,'ACTIVE')",
                employee, "Hugo", employee + "@example.com", department, position);
    }

    @AfterEach void cleanup() {
        jdbc.update("delete from outbox_events where employee_id = ?", employee);
        jdbc.update("delete from employees where id = ?", employee);
        jdbc.update("delete from positions where id = ?", position);
        jdbc.update("delete from departments where id = ?", department);
    }

    private HttpResponse<String> delete(String id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/employees/" + id)).DELETE().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test void deactivatesOnceAndPreservesRecordOnRepeatedRequest() throws Exception {
        assertEquals(204, delete(employee.toString()).statusCode());
        assertEquals("INACTIVE", jdbc.queryForObject("select status from employees where id = ?", String.class, employee));
        assertEquals(1L, jdbc.queryForObject("select version from employees where id = ?", Long.class, employee));
        assertEquals(204, delete(employee.toString()).statusCode());
        assertEquals(1L, jdbc.queryForObject("select version from employees where id = ?", Long.class, employee));
        assertEquals("Hugo", jdbc.queryForObject("select name from employees where id = ?", String.class, employee));
        assertEquals(1, jdbc.queryForObject("select count(*) from outbox_events where employee_id = ? and event_type = 'EmployeeStatusChangedEvent'",
                Integer.class, employee));
    }

    @Test void distinguishesMissingAndInvalidId() throws Exception {
        assertEquals(404, delete(UUID.randomUUID().toString()).statusCode());
        assertEquals(400, delete("invalid-uuid").statusCode());
        assertEquals(0, jdbc.queryForObject("select count(*) from outbox_events where employee_id = ?", Integer.class, employee));
    }

    @Test void concurrentRequestsRemainIdempotent() throws Exception {
        var first = CompletableFuture.supplyAsync(this::deleteUnchecked);
        var second = CompletableFuture.supplyAsync(this::deleteUnchecked);
        assertEquals(204, first.get(15, TimeUnit.SECONDS));
        assertEquals(204, second.get(15, TimeUnit.SECONDS));
        assertEquals("INACTIVE", jdbc.queryForObject("select status from employees where id = ?", String.class, employee));
        assertEquals(1L, jdbc.queryForObject("select version from employees where id = ?", Long.class, employee));
        assertEquals(1, jdbc.queryForObject("select count(*) from outbox_events where employee_id = ?", Integer.class, employee));
    }

    private int deleteUnchecked() {
        try { return delete(employee.toString()).statusCode(); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
