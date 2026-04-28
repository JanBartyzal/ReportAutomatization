package com.reportplatform.sink.tbl.service;

import com.reportplatform.sink.tbl.backend.PostgresTableStorageBackend;
import com.reportplatform.sink.tbl.backend.SparkTableStorageBackend;
import com.reportplatform.sink.tbl.backend.TableStorageBackend;
import com.reportplatform.sink.tbl.entity.StorageRoutingConfigEntity;
import com.reportplatform.sink.tbl.repository.StorageRoutingConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StorageRoutingService} rule-matching logic.
 *
 * <p>Specificity order under test (first match wins):
 * <ol>
 *   <li>org + source_type</li>
 *   <li>org only</li>
 *   <li>source_type only</li>
 *   <li>global default (both NULL)</li>
 *   <li>hard fallback → POSTGRES</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class StorageRoutingServiceTest {

    @Mock
    private StorageRoutingConfigRepository repository;

    @Mock
    private PostgresTableStorageBackend postgresBackend;

    @Mock
    private SparkTableStorageBackend sparkBackend;

    private StorageRoutingService service;

    private final UUID orgA = UUID.randomUUID();
    private final UUID orgB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(postgresBackend.backendType()).thenReturn("POSTGRES");
        when(sparkBackend.backendType()).thenReturn("SPARK");
    }

    private StorageRoutingService buildService(List<StorageRoutingConfigEntity> rules) {
        when(repository.findAllEffective(any())).thenReturn(rules);
        return new StorageRoutingService(repository, List.of(postgresBackend, sparkBackend));
    }

    // ---- helpers ----

    private StorageRoutingConfigEntity rule(UUID orgId, String sourceType, String backend) {
        StorageRoutingConfigEntity e = new StorageRoutingConfigEntity();
        e.setOrgId(orgId);
        e.setSourceType(sourceType);
        e.setBackend(backend);
        e.setEffectiveFrom(OffsetDateTime.now().minusMinutes(1));
        return e;
    }

    // ---- tests ----

    @Test
    void returnsPostgresWhenNoRulesConfigured() {
        service = buildService(List.of());
        TableStorageBackend result = service.resolve(orgA.toString(), "EXCEL");
        assertThat(result).isSameAs(postgresBackend);
    }

    @Test
    void orgAndSourceTypeRuleTakesPrecedenceOverAllOthers() {
        // Most-specific rule: orgA + EXCEL → SPARK
        // Less-specific rule: orgA only → POSTGRES
        // Global default: POSTGRES
        service = buildService(List.of(
                rule(orgA, "EXCEL", "SPARK"),    // specificity 0
                rule(orgA, null,    "POSTGRES"), // specificity 1
                rule(null, null,    "POSTGRES")  // specificity 3
        ));

        assertThat(service.resolve(orgA.toString(), "EXCEL")).isSameAs(sparkBackend);
    }

    @Test
    void orgOnlyRuleTakesPrecedenceOverSourceTypeAndGlobal() {
        service = buildService(List.of(
                rule(orgA, null,    "SPARK"),    // specificity 1 – org only
                rule(null, "EXCEL", "POSTGRES"), // specificity 2 – source only
                rule(null, null,    "POSTGRES")  // specificity 3 – global
        ));

        assertThat(service.resolve(orgA.toString(), "EXCEL")).isSameAs(sparkBackend);
    }

    @Test
    void sourceTypeRuleTakesPrecedenceOverGlobal() {
        service = buildService(List.of(
                rule(null, "SERVICE_NOW", "SPARK"),  // specificity 2
                rule(null, null,          "POSTGRES") // specificity 3
        ));

        // orgB has no specific rule – matches source_type rule
        assertThat(service.resolve(orgB.toString(), "SERVICE_NOW")).isSameAs(sparkBackend);
        // EXCEL has no rule at all – falls through to global POSTGRES
        assertThat(service.resolve(orgB.toString(), "EXCEL")).isSameAs(postgresBackend);
    }

    @Test
    void globalDefaultRuleUsedWhenNothingElseMatches() {
        service = buildService(List.of(
                rule(orgA, "EXCEL", "SPARK"),   // only matches orgA+EXCEL
                rule(null, null,    "SPARK")    // global default
        ));

        // orgB + CSV → no specific match → global default SPARK
        assertThat(service.resolve(orgB.toString(), "CSV")).isSameAs(sparkBackend);
    }

    @Test
    void orgRuleDoesNotMatchDifferentOrg() {
        service = buildService(List.of(
                rule(orgA, null, "SPARK")   // only orgA
        ));

        // orgB should fall back to POSTGRES (hard fallback)
        assertThat(service.resolve(orgB.toString(), "EXCEL")).isSameAs(postgresBackend);
    }

    @Test
    void sourceTypeMatchingIsCaseInsensitive() {
        service = buildService(List.of(
                rule(null, "excel", "SPARK")
        ));

        assertThat(service.resolve(orgA.toString(), "EXCEL")).isSameAs(sparkBackend);
        assertThat(service.resolve(orgA.toString(), "Excel")).isSameAs(sparkBackend);
    }

    @Test
    void nullOrgIdHandledGracefully() {
        service = buildService(List.of(
                rule(null, null, "SPARK") // global default
        ));

        assertThat(service.resolve(null, "EXCEL")).isSameAs(sparkBackend);
    }

    @Test
    void throwsWhenNoBackendsRegistered() {
        when(repository.findAllEffective(any())).thenReturn(List.of());

        // Construction with empty backend list is allowed (backends may register later)
        StorageRoutingService emptyService = new StorageRoutingService(repository, List.of());
        assertThatCode(() -> new StorageRoutingService(repository, List.of()))
                .doesNotThrowAnyException();

        // resolve() must throw because POSTGRES fallback backend is absent
        assertThatThrownBy(() -> emptyService.resolve(orgA.toString(), "EXCEL"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No storage backends registered");
    }
}
