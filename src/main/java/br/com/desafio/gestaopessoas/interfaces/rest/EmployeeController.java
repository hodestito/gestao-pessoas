package br.com.desafio.gestaopessoas.interfaces.rest;

import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import br.com.desafio.gestaopessoas.application.EmployeeRegistration;
import br.com.desafio.gestaopessoas.application.ListEmployees;
import br.com.desafio.gestaopessoas.application.SearchEmployeesByDepartment;
import br.com.desafio.gestaopessoas.application.EmployeeDeactivation;
import br.com.desafio.gestaopessoas.application.DeactivationResult;
import br.com.desafio.gestaopessoas.application.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Colaboradores", description = "Cadastro, consulta e inativação de colaboradores.")
public class EmployeeController {
    private final EmployeeRegistration registration;
    private final ListEmployees listEmployees;
    private final SearchEmployeesByDepartment searchEmployeesByDepartment;
    private final EmployeeDeactivation deactivation;
    public EmployeeController(EmployeeRegistration registration, ListEmployees listEmployees,
                              SearchEmployeesByDepartment searchEmployeesByDepartment,
                              EmployeeDeactivation deactivation) {
        this.registration = registration;
        this.listEmployees = listEmployees;
        this.searchEmployeesByDepartment = searchEmployeesByDepartment;
        this.deactivation = deactivation;
    }

    @Schema(name = "EmployeeRequest", description = "Dados fornecidos pelo cliente; ID, data e estado são definidos pelo servidor.")
    record EmployeeRequest(
            @Schema(example = "Ana Silva") @NotBlank @Size(max = 120) String name,
            @Schema(description = "Normalizado sem espaços externos e em minúsculas. Único inclusive entre inativos. "
                    + "Após normalização, até 254 caracteres e formato ASCII simplificado.", example = "ana.silva@example.com")
            @NotBlank String email,
            @Schema(description = "ID de departamento existente.", example = "11111111-1111-4111-8111-111111111111")
            @NotNull UUID departmentId,
            @Schema(description = "ID de cargo existente.", example = "22222222-2222-4222-8222-222222222222")
            @NotNull UUID positionId) {}
    @Schema(name = "EmployeeResponse", requiredProperties = {"id", "name", "email", "departmentId", "positionId", "createdAt", "status"})
    record EmployeeResponse(UUID id, String name, String email, UUID departmentId, UUID positionId,
                            Instant createdAt, EmployeeStatus status) {
        static EmployeeResponse from(Employee e) {
            return new EmployeeResponse(e.id(), e.name(), e.email().value(), e.departmentId(), e.positionId(), e.createdAt(), e.status());
        }
    }
    @Schema(name = "EmployeePageResponse", description = "Página ordenada por createdAt ASC e id ASC. "
            + "Inclusões/remoções entre requisições podem deslocar resultados.",
            requiredProperties = {"content", "page", "size", "totalElements", "totalPages"})
    record EmployeePageResponse(List<EmployeeResponse> content,
                                @Schema(minimum = "0", description = "Página solicitada, iniciada em zero.") int page,
                                @Schema(minimum = "1", maximum = "100") int size,
                                @Schema(minimum = "0") long totalElements,
                                @Schema(minimum = "0", description = "Zero se não há resultados; página além da última retorna content vazio.") int totalPages) {
        static EmployeePageResponse from(PageResult<Employee> result) {
            return new EmployeePageResponse(result.content().stream().map(EmployeeResponse::from).toList(),
                    result.page(), result.size(), result.totalElements(), result.totalPages());
        }
    }

    @PostMapping("/employees")
    @Operation(operationId = "createEmployee", summary = "Cadastrar colaborador",
            description = "Cria em ACTIVE, usando departamento/cargo já cadastrados. "
                    + "201 confirma colaborador e evento na Outbox na mesma transação; histórico Kafka é assíncrono.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cadastro confirmado.",
                headers = @Header(name = "Location", description = "URI relativa do colaborador criado.", schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmployeeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Entrada inválida ou JSON malformado.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/BadRequest"))),
        @ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_EXISTS, inclusive para empregado inativo.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmployeeExceptionHandler.ErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "DEPARTMENT_NOT_FOUND ou POSITION_NOT_FOUND.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmployeeExceptionHandler.ErrorResponse.class))),
        @ApiResponse(responseCode = "415", description = "Tipo de conteúdo não suportado.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/FrameworkError"))),
        @ApiResponse(responseCode = "500", description = "Falha técnica inesperada.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/FrameworkError")))
    })
    ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        Employee e = registration.execute(request.name(), request.email(), request.departmentId(), request.positionId());
        return ResponseEntity.created(URI.create("/employees/" + e.id())).body(EmployeeResponse.from(e));
    }

    @GetMapping("/employees")
    @Operation(operationId = "listEmployees", summary = "Listar colaboradores",
            description = "Filtros opcionais combinados. Sem status, inclui ativos e inativos; sem resultados, retorna 200 com página vazia.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de colaboradores.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmployeePageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Paginação ou status inválido.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/BadRequest"))),
        @ApiResponse(responseCode = "500", description = "Falha técnica inesperada.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/FrameworkError")))
    })
    EmployeePageResponse list(
            @Parameter(description = "Nome parcial, sem diferenciar caixa. Espaços externos removidos; branco não filtra. "
                    + "%, _ e ! são literais. Sem garantia de equivalência de acentos.", example = "ana")
            @RequestParam(required = false) String name,
            @Parameter(description = "Sem o parâmetro, inclui ambos os estados.")
            @RequestParam(required = false) EmployeeStatus status,
            @Parameter(description = "Página iniciada em zero; page × size deve ser até 2147483647.", schema = @Schema(minimum = "0", defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página.", schema = @Schema(minimum = "1", maximum = "100", defaultValue = "20"))
            @RequestParam(defaultValue = "20") int size) {
        return EmployeePageResponse.from(listEmployees.execute(name, status, page, size));
    }

    @GetMapping("/employees/search")
    @Operation(operationId = "searchEmployeesByDepartment", summary = "Buscar por departamento",
            description = "Nome exato sem diferenciar caixa, após remover espaços externos; inclui departamentos homônimos "
                    + "e empregados ativos/inativos. Sem correspondência, 200 com página vazia.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de colaboradores dos departamentos correspondentes.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmployeePageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Department ausente/branco ou paginação inválida.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/BadRequest"))),
        @ApiResponse(responseCode = "500", description = "Falha técnica inesperada.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/FrameworkError")))
    })
    EmployeePageResponse searchByDepartment(
            @Parameter(description = "Nome completo do departamento, obrigatório e não branco.", example = "RH")
            @RequestParam String department,
            @Parameter(description = "Página iniciada em zero; page × size deve ser até 2147483647.", schema = @Schema(minimum = "0", defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página.", schema = @Schema(minimum = "1", maximum = "100", defaultValue = "20"))
            @RequestParam(defaultValue = "20") int size) {
        return EmployeePageResponse.from(searchEmployeesByDepartment.execute(department, page, size));
    }

    @DeleteMapping("/employees/{id}")
    @Operation(operationId = "deactivateEmployee", summary = "Inativar colaborador",
            description = "Preserva o cadastro. ACTIVE → INACTIVE registra evento na mesma transação; repetição retorna 204 "
                    + "sem nova mudança/evento lógico. Uma releitura após conflito otimista; segundo conflito retorna 409. "
                    + "204 não espera publicação Kafka nem processamento do histórico.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Inativado ou já inativo.", content = @Content),
        @ApiResponse(responseCode = "400", description = "ID malformado.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/BadRequest"))),
        @ApiResponse(responseCode = "404", description = "Colaborador inexistente, sem corpo.", content = @Content),
        @ApiResponse(responseCode = "409", description = "CONCURRENT_MODIFICATION após a segunda tentativa.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmployeeExceptionHandler.ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Falha técnica inesperada.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/FrameworkError")))
    })
    ResponseEntity<Void> deactivate(@Parameter(description = "UUID do colaborador.") @PathVariable UUID id) {
        if (deactivation.execute(id) == DeactivationResult.NOT_FOUND) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }
}
