package br.com.desafio.gestaopessoas;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrganizationApiIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;

    private HttpResponse<String> post(String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
    @Test void createsDepartmentAndPositionAndPersistsThem() throws Exception {
        var department = post("/departments", "{\"name\":\"RH\",\"costCenterCode\":\"CC-01\"}");
        assertEquals(201, department.statusCode());
        UUID departmentId = UUID.fromString(department.headers().firstValue("Location").orElseThrow().replace("/departments/", ""));
        try {
            assertEquals("RH", jdbc.queryForObject("select name from departments where id = ?", String.class, departmentId));
            var position = post("/positions", "{\"title\":\"Engenheiro\",\"level\":\"Sênior\"}");
            assertEquals(201, position.statusCode());
            UUID positionId = UUID.fromString(position.headers().firstValue("Location").orElseThrow().replace("/positions/", ""));
            try {
                assertEquals("Sênior", jdbc.queryForObject("select level from positions where id = ?", String.class, positionId));
            } finally {
                jdbc.update("delete from positions where id = ?", positionId);
            }
        } finally {
            jdbc.update("delete from departments where id = ?", departmentId);
        }
    }
    @Test void rejectsInvalidRequests() throws Exception {
        assertEquals(400, post("/departments", "{\"name\":\" \",\"costCenterCode\":\"CC-01\"}").statusCode());
        assertEquals(400, post("/positions", "{\"title\":\"Analista\",\"level\":\"\"}").statusCode());
    }
}
