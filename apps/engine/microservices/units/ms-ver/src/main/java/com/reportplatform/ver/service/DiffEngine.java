package com.reportplatform.ver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.reportplatform.ver.model.dto.FieldChange;
import com.reportplatform.ver.model.enums.ChangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class DiffEngine {

    private static final Logger log = LoggerFactory.getLogger(DiffEngine.class);
    private static final Pattern MONETARY_PATTERN = Pattern.compile(
            ".*(amount|cost|revenue|budget|expense|price|total|sum|fee).*",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public DiffEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<FieldChange> computeDiff(String snapshotV1Json, String snapshotV2Json) {
        try {
            JsonNode v1 = objectMapper.readTree(snapshotV1Json);
            JsonNode v2 = objectMapper.readTree(snapshotV2Json);
            List<FieldChange> changes = new ArrayList<>();
            compareNodes(v1, v2, "", changes);
            return changes;
        } catch (Exception e) {
            log.error("Error computing diff", e);
            return List.of();
        }
    }

    private void compareNodes(JsonNode v1, JsonNode v2, String path, List<FieldChange> changes) {
        if (v1 == null && v2 == null) return;

        if (v1 == null || v1.isNull()) {
            changes.add(new FieldChange(path, ChangeType.ADDED, null, nodeToValue(v2)));
            return;
        }
        if (v2 == null || v2.isNull()) {
            changes.add(new FieldChange(path, ChangeType.REMOVED, nodeToValue(v1), null));
            return;
        }

        if (v1.isObject() && v2.isObject()) {
            compareObjects((ObjectNode) v1, (ObjectNode) v2, path, changes);
        } else if (v1.isArray() && v2.isArray()) {
            compareArrays((ArrayNode) v1, (ArrayNode) v2, path, changes);
        } else if (!v1.equals(v2)) {
            changes.add(new FieldChange(path, ChangeType.MODIFIED, nodeToValue(v1), nodeToValue(v2)));
        }
    }

    private void compareObjects(ObjectNode v1, ObjectNode v2, String basePath, List<FieldChange> changes) {
        Set<String> allFields = new LinkedHashSet<>();
        v1.fieldNames().forEachRemaining(allFields::add);
        v2.fieldNames().forEachRemaining(allFields::add);

        for (String field : allFields) {
            String fieldPath = basePath.isEmpty() ? field : basePath + "." + field;
            JsonNode val1 = v1.get(field);
            JsonNode val2 = v2.get(field);
            compareNodes(val1, val2, fieldPath, changes);
        }
    }

    private void compareArrays(ArrayNode v1, ArrayNode v2, String basePath, List<FieldChange> changes) {
        int maxLen = Math.max(v1.size(), v2.size());
        for (int i = 0; i < maxLen; i++) {
            String indexPath = basePath + "[" + i + "]";
            JsonNode elem1 = i < v1.size() ? v1.get(i) : null;
            JsonNode elem2 = i < v2.size() ? v2.get(i) : null;
            compareNodes(elem1, elem2, indexPath, changes);
        }
    }

    private Object nodeToValue(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) return node.textValue();
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.booleanValue();
        return node.toString();
    }

    public static boolean isMonetaryField(String fieldPath) {
        return MONETARY_PATTERN.matcher(fieldPath).matches();
    }
}
