package br.com.desafio.gestaopessoas;

import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import br.com.desafio.gestaopessoas.support.WithIntegrationContainers;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@WithIntegrationContainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiIntegrationTest {
    @LocalServerPort int port;
    @Autowired JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode spec() throws Exception {
        var response = get("/v3/api-docs");
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    @Test void exposesExactlyTheSixBusinessOperationsWithExplicitResponseCodes() throws Exception {
        var document = spec();
        assertTrue(document.path("openapi").asText().startsWith("3."));
        assertReferencesResolve(document, document);
        assertEquals("Gestão de Pessoas API", document.path("info").path("title").asText());
        var paths = document.path("paths");
        assertEquals(Set.of("/employees", "/employees/search", "/employees/{id}", "/departments", "/positions"), names(paths));
        var contracts = Map.of(
                "/employees#get", Set.of("200", "400", "500"),
                "/employees#post", Set.of("201", "400", "409", "422", "415", "500"),
                "/employees/search#get", Set.of("200", "400", "500"),
                "/employees/{id}#delete", Set.of("204", "400", "404", "409", "500"),
                "/departments#post", Set.of("201", "400", "415", "500"),
                "/positions#post", Set.of("201", "400", "415", "500"));
        int count = 0;
        for (var entry : paths.properties()) {
            for (String method : List.of("get", "post", "delete", "put", "patch")) {
                if (!entry.getValue().has(method)) continue;
                count++;
                String key = entry.getKey() + "#" + method;
                assertTrue(contracts.containsKey(key), key);
                assertEquals(contracts.get(key), names(entry.getValue().path(method).path("responses")), key);
            }
        }
        assertEquals(6, count);
        var deletion = paths.path("/employees/{id}").path("delete").path("responses");
        assertTrue(deletion.path("204").path("content").isMissingNode());
        assertTrue(deletion.path("404").path("content").isMissingNode());
        for (String path : List.of("/employees", "/departments", "/positions")) {
            assertTrue(paths.path(path).path("post").path("responses").path("201").path("headers").has("Location"));
        }
    }

    @Test void describesPaginationReferencesRequiredInputsAndDomainEnums() throws Exception {
        var document = spec();
        var parameters = document.path("paths").path("/employees").path("get").path("parameters");
        assertEquals(0, parameter(parameters, "page").path("schema").path("default").asInt());
        assertEquals(0, parameter(parameters, "page").path("schema").path("minimum").asInt());
        var size = parameter(parameters, "size").path("schema");
        assertEquals(20, size.path("default").asInt());
        assertEquals(1, size.path("minimum").asInt());
        assertEquals(100, size.path("maximum").asInt());
        assertEquals(Set.of("ACTIVE", "INACTIVE"), strings(resolve(document, parameter(parameters, "status").path("schema")).path("enum")));
        var department = parameter(document.path("paths").path("/employees/search").path("get").path("parameters"), "department");
        assertTrue(department.path("required").asBoolean());
        var schemas = document.path("components").path("schemas");
        var employee = schemas.path("EmployeeRequest");
        assertEquals(Set.of("name", "email", "departmentId", "positionId"), strings(employee.path("required")));
        assertEquals(120, employee.path("properties").path("name").path("maxLength").asInt());
        assertEquals("uuid", employee.path("properties").path("departmentId").path("format").asText());
        assertEquals("uuid", employee.path("properties").path("positionId").path("format").asText());
        var page = schemas.path("EmployeePageResponse");
        assertEquals(Set.of("content", "page", "size", "totalElements", "totalPages"), names(page.path("properties")));
        assertEquals("#/components/schemas/EmployeeResponse", page.path("properties").path("content").path("items").path("$ref").asText());
        var response = schemas.path("EmployeeResponse").path("properties");
        assertEquals("date-time", response.path("createdAt").path("format").asText());
        assertEquals("uuid", response.path("id").path("format").asText());
        assertEquals(50, schemas.path("DepartmentRequest").path("properties").path("costCenterCode").path("maxLength").asInt());
        assertEquals(50, schemas.path("PositionRequest").path("properties").path("level").path("maxLength").asInt());
    }

    @Test void documentsBothObservedBadRequestShapesAndKeepsMissingEmployeeBodyEmpty() throws Exception {
        var document = spec();
        var alternatives = strings(document.path("components").path("schemas").path("BadRequest").path("oneOf")
                .valueStream().map(node -> node.path("$ref")).toList());
        assertEquals(Set.of("#/components/schemas/ApiError", "#/components/schemas/FrameworkError"), alternatives);
        var domainFailure = get("/employees?page=-1");
        assertEquals(400, domainFailure.statusCode());
        assertEquals(Set.of("code", "message"), names(json.readTree(domainFailure.body())));
        assertEquals("INVALID_INPUT", json.readTree(domainFailure.body()).path("code").asText());
        var frameworkFailure = get("/employees/search");
        assertEquals(400, frameworkFailure.statusCode());
        var actual = json.readTree(frameworkFailure.body());
        assertTrue(names(actual).containsAll(strings(document.path("components").path("schemas").path("FrameworkError").path("required"))));
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/employees/" + UUID.randomUUID())).DELETE().build();
        var missing = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, missing.statusCode());
        assertEquals("", missing.body());
    }

    @Test void servesSwaggerUiAssetsConfigurationAndYamlLocally() throws Exception {
        var page = get("/swagger-ui.html");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("swagger-ui"));
        var config = get("/v3/api-docs/swagger-config");
        assertEquals(200, config.statusCode());
        assertEquals("/v3/api-docs", json.readTree(config.body()).path("url").asText());
        assertEquals(200, get("/swagger-ui/swagger-ui-bundle.js").statusCode());
        var yaml = get("/v3/api-docs.yaml");
        assertEquals(200, yaml.statusCode());
        assertTrue(yaml.body().contains("/employees:"));
    }

    private static Set<String> names(JsonNode object) {
        var result = new HashSet<String>();
        for (var property : object.properties()) result.add(property.getKey());
        return result;
    }

    private static Set<String> strings(Iterable<JsonNode> values) {
        var result = new HashSet<String>();
        for (var value : values) result.add(value.asText());
        return result;
    }

    private static JsonNode parameter(JsonNode parameters, String name) {
        for (var parameter : parameters) if (name.equals(parameter.path("name").asText())) return parameter;
        throw new AssertionError("Missing parameter: " + name);
    }

    private static JsonNode resolve(JsonNode document, JsonNode schema) {
        if (schema.has("$ref")) return document.at(schema.path("$ref").asText().substring(1));
        return schema;
    }

    private static void assertReferencesResolve(JsonNode document, JsonNode node) {
        if (node.has("$ref")) {
            String reference = node.path("$ref").asText();
            assertTrue(reference.startsWith("#/"), reference);
            assertFalse(document.at(reference.substring(1)).isMissingNode(), reference);
        }
        if (node.isObject()) {
            for (var property : node.properties()) assertReferencesResolve(document, property.getValue());
        } else if (node.isArray()) {
            for (var item : node) assertReferencesResolve(document, item);
        }
    }
}
