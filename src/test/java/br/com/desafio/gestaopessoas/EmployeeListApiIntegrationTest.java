package br.com.desafio.gestaopessoas;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
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
        properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=br.com.desafio.gestaopessoas.EmployeeListApiIntegrationTest$SqlCapture")
class EmployeeListApiIntegrationTest {
    public static class SqlCapture implements StatementInspector {
        static final ConcurrentLinkedQueue<String> statements = new ConcurrentLinkedQueue<>();
        @Override public String inspect(String sql) { statements.add(sql); return sql; }
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final UUID department = UUID.randomUUID();
    private final UUID position = UUID.randomUUID();
    private final UUID first = new UUID(0x1000L, UUID.randomUUID().getLeastSignificantBits());
    private final UUID second = new UUID(0x2000L, UUID.randomUUID().getLeastSignificantBits());
    private final UUID third = new UUID(0x3000L, UUID.randomUUID().getLeastSignificantBits());
    private final String marker = "Busca" + UUID.randomUUID().toString().replace("-", "");

    @BeforeEach void setup() {
        jdbc.update("insert into departments(id,name,cost_center_code) values(?,?,?)", department, "RH", "test");
        jdbc.update("insert into positions(id,title,level) values(?,?,?)", position, "Dev", "Senior");
        insert(first, marker + " Hugo", "ACTIVE");
        insert(second, marker + " HUGO", "INACTIVE");
        insert(third, marker + " %literal_!end", "ACTIVE");
    }

    private void insert(UUID id, String name, String status) {
        jdbc.update("insert into employees(id,name,email,department_id,position_id,created_at,status) values(?,?,?,?,?,?,?)",
                id, name, id + "@example.com", department, position,
                Timestamp.from(Instant.parse("2026-10-27T12:00:00Z")), status);
    }

    @AfterEach void cleanup() {
        for (UUID id : new UUID[]{first, second, third}) jdbc.update("delete from employees where id = ?", id);
        jdbc.update("delete from positions where id = ?", position);
        jdbc.update("delete from departments where id = ?", department);
    }

    private HttpResponse<String> get(String query) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/employees" + query)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    @Test void paginatesBothStatusesWithStableTieBreakerAndOwnResponse() throws Exception {
        var page0 = get("?name=" + encode(marker) + "&page=0&size=2");
        assertEquals(200, page0.statusCode());
        String body = page0.body();
        assertTrue(body.contains(first.toString()));
        assertTrue(body.contains(second.toString()));
        assertFalse(body.contains(third.toString()));
        assertTrue(body.indexOf(first.toString()) < body.indexOf(second.toString()));
        assertTrue(body.contains("\"page\":0"));
        assertTrue(body.contains("\"size\":2"));
        assertTrue(body.contains("\"totalElements\":3"));
        assertTrue(body.contains("\"totalPages\":2"));
        var page1 = get("?name=" + encode(marker) + "&page=1&size=2");
        assertTrue(page1.body().contains(third.toString()));
        assertFalse(page1.body().contains(first.toString()));
        assertTrue(page1.body().contains("\"page\":1"));
        var beyond = get("?name=" + encode(marker) + "&page=9&size=2");
        assertEquals(200, beyond.statusCode());
        assertTrue(beyond.body().contains("\"content\":[]"));
        assertTrue(beyond.body().contains("\"page\":9"));
        assertTrue(beyond.body().contains("\"totalElements\":3"));
        assertTrue(beyond.body().contains("\"totalPages\":2"));
    }

    @Test void listsBothStatusesWithoutFiltersAndTreatsBlankNameAsAbsent() throws Exception {
        for (String query : new String[]{"?size=100", "?name=%20%20&size=100"}) {
            var response = get(query);
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains(first.toString()));
            assertTrue(response.body().contains(second.toString()));
            assertTrue(response.body().contains(third.toString()));
            assertTrue(response.body().contains("\"page\":0"));
        }
    }

    @Test void filtersNameIgnoringCaseAndCombinesStatus() throws Exception {
        var response = get("?name=" + encode(marker.toLowerCase(java.util.Locale.ROOT) + " hugo") + "&status=INACTIVE");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains(second.toString()));
        assertFalse(response.body().contains(first.toString()));
        assertTrue(response.body().contains("\"totalElements\":1"));
        assertTrue(response.body().contains("\"size\":20"));
    }

    @Test void interpretsWildcardCharactersAsLiteralText() throws Exception {
        var response = get("?name=" + encode(marker + " %"));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains(third.toString()));
        assertTrue(response.body().contains("\"totalElements\":1"));
        var underscore = get("?name=" + encode(marker + " _"));
        assertEquals(200, underscore.statusCode());
        assertTrue(underscore.body().contains("\"content\":[]"));
        var exclamation = get("?name=" + encode(marker + " %literal_!end"));
        assertEquals(200, exclamation.statusCode());
        assertTrue(exclamation.body().contains(third.toString()));
        assertTrue(exclamation.body().contains("\"totalElements\":1"));
    }

    @Test void treatsSqlInjectionPayloadAsDataAndBindsTheLikeValue() throws Exception {
        String payload = "' OR 1=1 --";
        SqlCapture.statements.clear();
        var response = get("?name=" + encode(payload));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"content\":[]"));
        assertTrue(response.body().contains("\"totalElements\":0"));
        assertEquals(3, jdbc.queryForObject("select count(*) from employees where id in (?,?,?)", Integer.class, first, second, third));
        String sql = SqlCapture.statements.stream()
                .filter(statement -> statement.toLowerCase(java.util.Locale.ROOT).contains(" like "))
                .findFirst().orElseThrow();
        assertTrue(sql.contains("?"), sql);
        assertFalse(sql.toLowerCase(java.util.Locale.ROOT).contains(payload.toLowerCase(java.util.Locale.ROOT)), sql);
    }

    @Test void returnsEmptyPageAndRejectsInvalidParameters() throws Exception {
        var empty = get("?name=" + encode("absent-" + UUID.randomUUID()));
        assertEquals(200, empty.statusCode());
        assertTrue(empty.body().contains("\"content\":[]"));
        assertTrue(empty.body().contains("\"totalElements\":0"));
        assertEquals(400, get("?page=-1").statusCode());
        assertEquals(400, get("?size=0").statusCode());
        assertEquals(400, get("?size=101").statusCode());
        assertEquals(400, get("?page=2147483647&size=100").statusCode());
        assertEquals(400, get("?status=UNKNOWN").statusCode());
        assertEquals(400, get("?page=abc").statusCode());
    }
}
