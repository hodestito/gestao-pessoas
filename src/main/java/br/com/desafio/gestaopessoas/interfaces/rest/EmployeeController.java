package br.com.desafio.gestaopessoas.interfaces.rest;

import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import br.com.desafio.gestaopessoas.application.EmployeeRegistration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class EmployeeController {
    private final EmployeeRegistration registration;
    public EmployeeController(EmployeeRegistration registration) { this.registration = registration; }

    record EmployeeRequest(@NotBlank @Size(max = 120) String name, @NotBlank String email,
                           @NotNull UUID departmentId, @NotNull UUID positionId) {}
    record EmployeeResponse(UUID id, String name, String email, UUID departmentId, UUID positionId,
                            Instant createdAt, EmployeeStatus status) {
        static EmployeeResponse from(Employee e) {
            return new EmployeeResponse(e.id(), e.name(), e.email().value(), e.departmentId(), e.positionId(), e.createdAt(), e.status());
        }
    }

    @PostMapping("/employees")
    ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        Employee e = registration.execute(request.name(), request.email(), request.departmentId(), request.positionId());
        return ResponseEntity.created(URI.create("/employees/" + e.id())).body(EmployeeResponse.from(e));
    }
}
