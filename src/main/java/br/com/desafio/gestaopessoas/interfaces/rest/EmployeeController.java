package br.com.desafio.gestaopessoas.interfaces.rest;

import br.com.desafio.gestaopessoas.domain.employee.Employee;
import br.com.desafio.gestaopessoas.domain.employee.EmployeeStatus;
import br.com.desafio.gestaopessoas.application.EmployeeRegistration;
import br.com.desafio.gestaopessoas.application.ListEmployees;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class EmployeeController {
    private final EmployeeRegistration registration;
    private final ListEmployees listEmployees;
    public EmployeeController(EmployeeRegistration registration, ListEmployees listEmployees) {
        this.registration = registration;
        this.listEmployees = listEmployees;
    }

    record EmployeeRequest(@NotBlank @Size(max = 120) String name, @NotBlank String email,
                           @NotNull UUID departmentId, @NotNull UUID positionId) {}
    record EmployeeResponse(UUID id, String name, String email, UUID departmentId, UUID positionId,
                            Instant createdAt, EmployeeStatus status) {
        static EmployeeResponse from(Employee e) {
            return new EmployeeResponse(e.id(), e.name(), e.email().value(), e.departmentId(), e.positionId(), e.createdAt(), e.status());
        }
    }
    record EmployeePageResponse(List<EmployeeResponse> content, int page, int size, long totalElements, int totalPages) {
        static EmployeePageResponse from(Page<Employee> result) {
            return new EmployeePageResponse(result.getContent().stream().map(EmployeeResponse::from).toList(),
                    result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
        }
    }

    @PostMapping("/employees")
    ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        Employee e = registration.execute(request.name(), request.email(), request.departmentId(), request.positionId());
        return ResponseEntity.created(URI.create("/employees/" + e.id())).body(EmployeeResponse.from(e));
    }

    @GetMapping("/employees")
    EmployeePageResponse list(@RequestParam(required = false) String name,
                              @RequestParam(required = false) EmployeeStatus status,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        return EmployeePageResponse.from(listEmployees.execute(name, status, page, size));
    }
}
