package com.reportplatform.ver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reportplatform.ver.model.dto.FieldChange;
import com.reportplatform.ver.model.enums.ChangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiffEngineTest {

    private DiffEngine diffEngine;

    @BeforeEach
    void setUp() {
        diffEngine = new DiffEngine(new ObjectMapper());
    }

    @Test
    void shouldDetectModifiedFields() {
        String v1 = """
                {"name": "Report A", "value": 100}
                """;
        String v2 = """
                {"name": "Report B", "value": 100}
                """;

        List<FieldChange> changes = diffEngine.computeDiff(v1, v2);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).fieldPath()).isEqualTo("name");
        assertThat(changes.get(0).changeType()).isEqualTo(ChangeType.MODIFIED);
        assertThat(changes.get(0).oldValue()).isEqualTo("Report A");
        assertThat(changes.get(0).newValue()).isEqualTo("Report B");
    }

    @Test
    void shouldDetectAddedFields() {
        String v1 = """
                {"name": "Report A"}
                """;
        String v2 = """
                {"name": "Report A", "status": "DRAFT"}
                """;

        List<FieldChange> changes = diffEngine.computeDiff(v1, v2);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).fieldPath()).isEqualTo("status");
        assertThat(changes.get(0).changeType()).isEqualTo(ChangeType.ADDED);
    }

    @Test
    void shouldDetectRemovedFields() {
        String v1 = """
                {"name": "Report A", "status": "DRAFT"}
                """;
        String v2 = """
                {"name": "Report A"}
                """;

        List<FieldChange> changes = diffEngine.computeDiff(v1, v2);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).fieldPath()).isEqualTo("status");
        assertThat(changes.get(0).changeType()).isEqualTo(ChangeType.REMOVED);
    }

    @Test
    void shouldHandleNestedObjects() {
        String v1 = """
                {"data": {"amount": 1000, "category": "IT"}}
                """;
        String v2 = """
                {"data": {"amount": 1500, "category": "IT"}}
                """;

        List<FieldChange> changes = diffEngine.computeDiff(v1, v2);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).fieldPath()).isEqualTo("data.amount");
        assertThat(changes.get(0).changeType()).isEqualTo(ChangeType.MODIFIED);
    }

    @Test
    void shouldHandleArrayChanges() {
        String v1 = """
                {"rows": [{"id": 1, "value": 100}, {"id": 2, "value": 200}]}
                """;
        String v2 = """
                {"rows": [{"id": 1, "value": 150}, {"id": 2, "value": 200}, {"id": 3, "value": 300}]}
                """;

        List<FieldChange> changes = diffEngine.computeDiff(v1, v2);

        assertThat(changes).hasSizeGreaterThanOrEqualTo(2);
        assertThat(changes.stream().anyMatch(c -> c.fieldPath().equals("rows[0].value"))).isTrue();
        assertThat(changes.stream().anyMatch(c -> c.changeType() == ChangeType.ADDED)).isTrue();
    }

    @Test
    void shouldReturnEmptyForIdenticalSnapshots() {
        String json = """
                {"name": "Report A", "value": 100}
                """;

        List<FieldChange> changes = diffEngine.computeDiff(json, json);

        assertThat(changes).isEmpty();
    }

    @Test
    void shouldIdentifyMonetaryFields() {
        assertThat(DiffEngine.isMonetaryField("data.total_amount")).isTrue();
        assertThat(DiffEngine.isMonetaryField("it_cost")).isTrue();
        assertThat(DiffEngine.isMonetaryField("revenue")).isTrue();
        assertThat(DiffEngine.isMonetaryField("name")).isFalse();
    }
}
