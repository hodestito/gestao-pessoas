package br.com.desafio.gestaopessoas.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, UUID>, JpaSpecificationExecutor<EmployeeEntity> {
    boolean existsByEmail(String email);

    @Query("select e from EmployeeEntity e where exists "
            + "(select d.id from DepartmentEntity d where d.id = e.departmentId and lower(d.name) = lower(:department))")
    Page<EmployeeEntity> searchByDepartment(@Param("department") String department, Pageable pageable);
}
