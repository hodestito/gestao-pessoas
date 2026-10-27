package br.com.desafio.gestaopessoas;

import br.com.desafio.gestaopessoas.application.CreateDepartment;
import br.com.desafio.gestaopessoas.application.CreatePosition;
import br.com.desafio.gestaopessoas.application.DepartmentRepository;
import br.com.desafio.gestaopessoas.application.PositionRepository;
import br.com.desafio.gestaopessoas.domain.department.Department;
import br.com.desafio.gestaopessoas.domain.position.Position;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationDomainTest {
    @Test void createsAndNormalizesDepartment() {
        DepartmentRepository repository = org.mockito.Mockito.mock(DepartmentRepository.class);
        org.mockito.Mockito.when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        var department = new CreateDepartment(repository).execute("  RH  ", " CC-01 ");
        assertNotNull(department.id());
        assertEquals("RH", department.name());
        assertEquals("CC-01", department.costCenterCode().value());
    }
    @Test void createsAndNormalizesPosition() {
        PositionRepository repository = org.mockito.Mockito.mock(PositionRepository.class);
        org.mockito.Mockito.when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        var position = new CreatePosition(repository).execute(" Engenheiro ", " Sênior ");
        assertNotNull(position.id());
        assertEquals("Engenheiro", position.title());
        assertEquals("Sênior", position.level());
    }
    @Test void rejectsInvalidValuesBeforeSaving() {
        DepartmentRepository repository = org.mockito.Mockito.mock(DepartmentRepository.class);
        var useCase = new CreateDepartment(repository);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(" ", "1"));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("RH", " "));
        org.mockito.Mockito.verifyNoInteractions(repository);
        assertThrows(IllegalArgumentException.class, () -> Position.create("x".repeat(121), "Senior"));
        assertThrows(IllegalArgumentException.class, () -> Department.create(null, "1"));
    }
}
