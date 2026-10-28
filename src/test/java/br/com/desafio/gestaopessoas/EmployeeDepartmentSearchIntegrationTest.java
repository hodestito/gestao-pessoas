package br.com.desafio.gestaopessoas;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.hibernate.resource.jdbc.spi.StatementInspector;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=br.com.desafio.gestaopessoas.EmployeeDepartmentSearchIntegrationTest$SqlCapture")
class EmployeeDepartmentSearchIntegrationTest {
    public static class SqlCapture implements StatementInspector {
        static final ConcurrentLinkedQueue<String> statements = new ConcurrentLinkedQueue<>();
        @Override public String inspect(String sql) { statements.add(sql); return sql; }
    }

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final UUID department1 = UUID.randomUUID();
    private final UUID department2 = UUID.randomUUID();
    private final UUID unrelated = UUID.randomUUID();
    private final UUID position = UUID.randomUUID();
    private final UUID first = new UUID(0x1000L, UUID.randomUUID().getLeastSignificantBits());
    private final UUID second = new UUID(0x2000L, UUID.randomUUID().getLeastSignificantBits());
    private final UUID third = new UUID(0x3000L, UUID.randomUUID().getLeastSignificantBits());
    private final String departmentName = "RH" + UUID.randomUUID().toString().replace("-", "");

    @BeforeEach void setup() {
        jdbc.update("insert into departments(id,name,cost_center_code) values(?,?,?)", department1, departmentName, "one");
        jdbc.update("insert into departments(id,name,cost_center_code) values(?,?,?)", department2, departmentName.toLowerCase(Locale.ROOT), "two");
        jdbc.update("insert into departments(id,name,cost_center_code) values(?,?,?)", unrelated, departmentName + " FILIAL", "three");
        jdbc.update("insert into positions(id,title,level) values(?,?,?)", position, "Dev", "Senior");
        insert(first, department1, "ACTIVE");
        insert(second, department2, "INACTIVE");
        insert(third, unrelated, "ACTIVE");
    }

    private void insert(UUID id, UUID department, String status) {
        jdbc.update("insert into employees(id,name,email,department_id,position_id,created_at,status) values(?,?,?,?,?,?,?)",
                id, "Hugo", id + "@example.com", department, position,
                Timestamp.from(Instant.parse("2026-10-27T12:00:00Z")), status);
    }

    @AfterEach void cleanup() {
        for (UUID id : new UUID[]{first, second, third}) jdbc.update("delete from employees where id = ?", id);
        jdbc.update("delete from positions where id = ?", position);
        for (UUID id : new UUID[]{department1, department2, unrelated}) jdbc.update("delete from departments where id = ?", id);
    }

    private HttpResponse<String> get(String query) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/employees/search" + query)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    @Test void findsBothStatusesAcrossDepartmentsWithSameName() throws Exception {
        var response = get("?department=" + encode(" " + departmentName.toLowerCase(Locale.ROOT) + " "));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains(first.toString()));
        assertTrue(response.body().contains(second.toString()));
        assertFalse(response.body().contains(third.toString()));
        assertTrue(response.body().contains("\"totalElements\":2"));
        assertTrue(response.body().contains("\"size\":20"));
    }

    @Test void returnsStablePagesAndEmptyForPartialOrUnknownName() throws Exception {
        String query = "?department=" + encode(departmentName) + "&size=1";
        var page0 = get(query + "&page=0");
        var page1 = get(query + "&page=1");
        assertEquals(200, page0.statusCode());
        assertEquals(200, page1.statusCode());
        assertTrue(page0.body().contains(first.toString()));
        assertTrue(page1.body().contains(second.toString()));
        assertTrue(page0.body().contains("\"totalPages\":2"));
        var missing = get("?department=" + encode(departmentName.substring(0, departmentName.length() - 1)));
        assertEquals(200, missing.statusCode());
        assertTrue(missing.body().contains("\"content\":[]"));
        assertTrue(missing.body().contains("\"totalElements\":0"));
        assertTrue(missing.body().contains("\"totalPages\":0"));
        var beyond = get(query + "&page=9");
        assertEquals(200, beyond.statusCode());
        assertTrue(beyond.body().contains("\"content\":[]"));
        assertTrue(beyond.body().contains("\"page\":9"));
        assertTrue(beyond.body().contains("\"totalElements\":2"));
        assertTrue(beyond.body().contains("\"totalPages\":2"));
    }

    @Test void rejectsMissingBlankAndInvalidPagination() throws Exception {
        assertEquals(400, get("").statusCode());
        assertEquals(400, get("?department=%20%20").statusCode());
        assertEquals(400, get("?department=" + encode(departmentName) + "&page=-1").statusCode());
        assertEquals(400, get("?department=" + encode(departmentName) + "&size=101").statusCode());
        assertEquals(400, get("?department=" + encode(departmentName) + "&page=2147483647&size=100").statusCode());
    }

    @Test void treatsSqlInjectionPayloadAsBoundData() throws Exception {
        String payload = "' OR 1=1 --";
        SqlCapture.statements.clear();
        var response = get("?department=" + encode(payload));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"content\":[]"));
        assertTrue(response.body().contains("\"totalElements\":0"));
        String sql = SqlCapture.statements.stream().filter(s -> s.toLowerCase(Locale.ROOT).contains("exists"))
                .findFirst().orElseThrow();
        assertTrue(sql.contains("?"), sql);
        assertFalse(sql.toLowerCase(Locale.ROOT).contains(payload.toLowerCase(Locale.ROOT)), sql);
    }
}
