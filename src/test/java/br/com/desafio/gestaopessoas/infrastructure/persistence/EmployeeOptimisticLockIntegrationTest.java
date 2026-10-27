package br.com.desafio.gestaopessoas.infrastructure.persistence;

import br.com.desafio.gestaopessoas.application.VersionedEmployee;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmployeeOptimisticLockIntegrationTest {
    @Autowired EmployeePersistenceAdapter employees;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    private final UUID department = UUID.randomUUID();
    private final UUID position = UUID.randomUUID();
    private final UUID employee = UUID.randomUUID();

    @BeforeEach void setup() {
        jdbc.update("insert into departments(id,name,cost_center_code) values(?,?,?)", department, "RH", "test");
        jdbc.update("insert into positions(id,title,level) values(?,?,?)", position, "Dev", "Senior");
        jdbc.update("insert into employees(id,name,email,department_id,position_id,created_at,status) values(?,?,?,?,?,current_timestamp,'ACTIVE')",
                employee, "Hugo", employee + "@example.com", department, position);
    }

    @AfterEach void cleanup() {
        jdbc.update("delete from employees where id = ?", employee);
        jdbc.update("delete from positions where id = ?", position);
        jdbc.update("delete from departments where id = ?", department);
    }

    @Test void staleVersionCannotOverwriteCommittedDeactivation() throws Exception {
        var bothRead = new CountDownLatch(2);
        var firstCommitted = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> {
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        VersionedEmployee snapshot = employees.findVersionedById(employee).orElseThrow();
                        bothRead.countDown();
                        await(bothRead);
                        snapshot.employee().deactivate();
                        employees.saveVersioned(snapshot);
                    });
                } finally { firstCommitted.countDown(); }
            });
            Future<?> second = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                VersionedEmployee snapshot = employees.findVersionedById(employee).orElseThrow();
                bothRead.countDown();
                await(bothRead);
                await(firstCommitted);
                snapshot.employee().deactivate();
                employees.saveVersioned(snapshot);
            }));
            first.get(15, TimeUnit.SECONDS);
            ExecutionException conflict = assertThrows(ExecutionException.class, () -> second.get(15, TimeUnit.SECONDS));
            assertInstanceOf(OptimisticLockingFailureException.class, conflict.getCause());
            assertEquals("INACTIVE", jdbc.queryForObject("select status from employees where id = ?", String.class, employee));
            assertEquals(1L, jdbc.queryForObject("select version from employees where id = ?", Long.class, employee));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("timeout aguardando transação de teste");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
