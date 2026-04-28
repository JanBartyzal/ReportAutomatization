package com.reportplatform.qry.service;

import com.reportplatform.qry.model.NamedQueryEntity;
import com.reportplatform.qry.model.dto.CreateNamedQueryRequest;
import com.reportplatform.qry.model.dto.NamedQueryExecuteRequest;
import com.reportplatform.qry.repository.NamedQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NamedQueryService}, focusing on the {@code assertReadOnly}
 * SQL validation and the create/update lifecycle.
 */
@ExtendWith(MockitoExtension.class)
class NamedQueryServiceTest {

    @Mock
    private NamedQueryRepository repository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query rlsQuery;

    private NamedQueryService service;

    @BeforeEach
    void setUp() {
        service = new NamedQueryService(repository);
        // Inject the EntityManager via reflection (field injection via @PersistenceContext)
        try {
            var emField = NamedQueryService.class.getDeclaredField("entityManager");
            emField.setAccessible(true);
            emField.set(service, entityManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // assertReadOnly — rejection (tested via create, which calls it first)
    // -------------------------------------------------------------------------

    @Nested
    class AssertReadOnly_Rejected {

        @ParameterizedTest(name = "rejects: {0}")
        @ValueSource(strings = {
                "DELETE FROM users",
                "delete from users",
                "DELETE\nFROM users",           // newline instead of space
                "DELETE\tFROM users",            // tab
                " DELETE FROM users",            // leading space
                "INSERT INTO t VALUES (1)",
                "UPDATE users SET name='x'",
                "DROP TABLE users",
                "ALTER TABLE t ADD COLUMN c INT",
                "TRUNCATE TABLE users",
                "CREATE TABLE x (id INT)",
                "EXECUTE sp_foo",
                "EXEC sp_foo",
                "CALL my_proc()",
                "GRANT SELECT ON t TO u",
                "REVOKE SELECT ON t FROM u",
                // Nested DML inside a CTE
                "WITH d AS (DELETE FROM users RETURNING *) SELECT * FROM d",
                // DML following a semi-colon (multi-statement attempt)
                "SELECT 1; DELETE FROM users",
        })
        void rejectsDmlAndDdlKeywords(String sql) {
            var req = new CreateNamedQueryRequest("q", null, sql, null, null);
            assertThatThrownBy(() -> service.create(UUID.randomUUID(), "user", req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden");
        }

        @Test
        void rejectsNonSelectStatement() {
            var req = new CreateNamedQueryRequest("q", null, "EXPLAIN SELECT 1", null, null);
            // EXPLAIN is not in our forbidden list, but it doesn't start with SELECT/WITH
            assertThatThrownBy(() -> service.create(UUID.randomUUID(), "user", req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SELECT statements only");
        }

        @Test
        void rejectsBlankSql() {
            var req = new CreateNamedQueryRequest("q", null, "  ", null, null);
            assertThatThrownBy(() -> service.create(UUID.randomUUID(), "user", req))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // assertReadOnly — identifiers that CONTAIN keywords must NOT be rejected
    // -------------------------------------------------------------------------

    @Nested
    class AssertReadOnly_Allowed {

        @BeforeEach
        void stubRlsAndRepo() {
            when(entityManager.createNativeQuery(anyString())).thenReturn(rlsQuery);
            when(rlsQuery.setParameter(anyString(), any())).thenReturn(rlsQuery);
            when(rlsQuery.getSingleResult()).thenReturn("ok");
            when(repository.existsByOrgIdAndName(any(), any())).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> {
                NamedQueryEntity e = inv.getArgument(0);
                return e;
            });
        }

        @ParameterizedTest(name = "allows: {0}")
        @ValueSource(strings = {
                "SELECT * FROM users",
                "SELECT update_count, deleted_at FROM audit_log",   // column names with keywords
                "SELECT * FROM truncate_history",                    // table name
                "WITH cte AS (SELECT 1) SELECT * FROM cte",
                "SELECT * FROM orders WHERE status = 'inserted'",   // keyword in string literal
        })
        void allowsSafeSelectQueries(String sql) {
            var req = new CreateNamedQueryRequest("q", null, sql, null, null);
            assertThatNoException().isThrownBy(
                    () -> service.create(UUID.randomUUID(), "user", req));
        }
    }

    // -------------------------------------------------------------------------
    // execute — assertReadOnly runs at runtime too
    // -------------------------------------------------------------------------

    @Nested
    class Execute_Validation {

        @Test
        void execute_rejectsInactiveQuery() {
            UUID orgId = UUID.randomUUID();
            UUID queryId = UUID.randomUUID();

            NamedQueryEntity inactive = buildEntity(queryId, orgId,
                    "SELECT 1", false, false);

            when(entityManager.createNativeQuery(anyString())).thenReturn(rlsQuery);
            when(rlsQuery.setParameter(anyString(), any())).thenReturn(rlsQuery);
            when(rlsQuery.getSingleResult()).thenReturn("ok");
            when(repository.findByIdAndOrgAccess(queryId, orgId)).thenReturn(Optional.of(inactive));

            assertThatThrownBy(() -> service.execute(orgId, queryId,
                    new NamedQueryExecuteRequest(null, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        void execute_rejectsSystemQueryIfSqlMutated() {
            // Simulates a scenario where a system query somehow has DML (should be caught at runtime)
            UUID orgId = UUID.randomUUID();
            UUID queryId = UUID.randomUUID();

            NamedQueryEntity withDml = buildEntity(queryId, orgId,
                    "DELETE FROM users", true, true);

            when(entityManager.createNativeQuery(anyString())).thenReturn(rlsQuery);
            when(rlsQuery.setParameter(anyString(), any())).thenReturn(rlsQuery);
            when(rlsQuery.getSingleResult()).thenReturn("ok");
            when(repository.findByIdAndOrgAccess(queryId, orgId)).thenReturn(Optional.of(withDml));

            assertThatThrownBy(() -> service.execute(orgId, queryId,
                    new NamedQueryExecuteRequest(null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("forbidden");
        }
    }

    // ---- helpers ----

    private NamedQueryEntity buildEntity(UUID id, UUID orgId, String sql,
                                          boolean active, boolean system) {
        NamedQueryEntity e = new NamedQueryEntity(
                orgId, "test-query", "desc", sql, "{}", "PLATFORM", system, "user");
        try {
            var idField = NamedQueryEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(e, id);
            var activeField = NamedQueryEntity.class.getDeclaredField("active");
            activeField.setAccessible(true);
            activeField.set(e, active);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return e;
    }
}
