package br.com.desafio.gestaopessoas.interfaces.rest;

import br.com.desafio.gestaopessoas.application.CreateDepartment;
import br.com.desafio.gestaopessoas.application.CreatePosition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrganizationController {
    private final CreateDepartment departments;
    private final CreatePosition positions;
    public OrganizationController(CreateDepartment departments, CreatePosition positions) {
        this.departments = departments; this.positions = positions;
    }
    @Schema(name = "DepartmentRequest", description = "Nome/código não possuem regra de unicidade neste cadastro.")
    record DepartmentRequest(@Schema(example = "RH") @NotBlank @Size(max=120) String name,
                             @Schema(example = "CC-01") @NotBlank @Size(max=50) String costCenterCode) {}
    @Schema(name = "PositionRequest", description = "Cargo e posição são sinônimos; nível é texto livre, sem enumeração fechada.")
    record PositionRequest(@Schema(example = "Engenheiro de Software") @NotBlank @Size(max=120) String title,
                           @Schema(example = "Sênior") @NotBlank @Size(max=50) String level) {}
    @Schema(name = "DepartmentResponse", requiredProperties = {"id", "name", "costCenterCode"})
    record DepartmentResponse(UUID id, String name, String costCenterCode) {}
    @Schema(name = "PositionResponse", requiredProperties = {"id", "title", "level"})
    record PositionResponse(UUID id, String title, String level) {}

    @PostMapping("/departments")
    @Tag(name = "Departamentos", description = "Cadastro de departamentos.")
    @Operation(operationId = "createDepartment", summary = "Cadastrar departamento",
            description = "Remove espaços externos e gera UUID. Nome até 120 e código de centro de custo até 50 caracteres. "
                    + "Não há evento de departamento neste escopo.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Departamento cadastrado.",
                headers = @Header(name = "Location", description = "URI relativa do departamento criado.", schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = DepartmentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Campos ausentes, brancos, acima dos limites ou JSON malformado.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/FrameworkError"))),
        @ApiResponse(responseCode = "415", description = "Tipo de conteúdo não suportado.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/FrameworkError"))),
        @ApiResponse(responseCode = "500", description = "Falha técnica inesperada.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/FrameworkError")))
    })
    ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        var department = departments.execute(request.name(), request.costCenterCode());
        return ResponseEntity.created(URI.create("/departments/" + department.id()))
                .body(new DepartmentResponse(department.id(), department.name(), department.costCenterCode().value()));
    }
    @PostMapping("/positions")
    @Tag(name = "Cargos", description = "Cadastro de cargos/posições.")
    @Operation(operationId = "createPosition", summary = "Cadastrar cargo",
            description = "Remove espaços externos e gera UUID. Título até 120 e nível até 50 caracteres. "
                    + "Não há evento de cargo neste escopo.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cargo cadastrado.",
                headers = @Header(name = "Location", description = "URI relativa do cargo criado.", schema = @Schema(type = "string")),
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PositionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Campos ausentes, brancos, acima dos limites ou JSON malformado.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/FrameworkError"))),
        @ApiResponse(responseCode = "415", description = "Tipo de conteúdo não suportado.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/FrameworkError"))),
        @ApiResponse(responseCode = "500", description = "Falha técnica inesperada.",
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/FrameworkError")))
    })
    ResponseEntity<PositionResponse> createPosition(@Valid @RequestBody PositionRequest request) {
        var position = positions.execute(request.title(), request.level());
        return ResponseEntity.created(URI.create("/positions/" + position.id()))
                .body(new PositionResponse(position.id(), position.title(), position.level()));
    }
}
