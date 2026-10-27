package br.com.desafio.gestaopessoas.interfaces.rest;

import br.com.desafio.gestaopessoas.application.CreateDepartment;
import br.com.desafio.gestaopessoas.application.CreatePosition;
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
    record DepartmentRequest(@NotBlank @Size(max=120) String name, @NotBlank @Size(max=50) String costCenterCode) {}
    record PositionRequest(@NotBlank @Size(max=120) String title, @NotBlank @Size(max=50) String level) {}
    record DepartmentResponse(UUID id, String name, String costCenterCode) {}
    record PositionResponse(UUID id, String title, String level) {}

    @PostMapping("/departments")
    ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        var department = departments.execute(request.name(), request.costCenterCode());
        return ResponseEntity.created(URI.create("/departments/" + department.id()))
                .body(new DepartmentResponse(department.id(), department.name(), department.costCenterCode().value()));
    }
    @PostMapping("/positions")
    ResponseEntity<PositionResponse> createPosition(@Valid @RequestBody PositionRequest request) {
        var position = positions.execute(request.title(), request.level());
        return ResponseEntity.created(URI.create("/positions/" + position.id()))
                .body(new PositionResponse(position.id(), position.title(), position.level()));
    }
}
