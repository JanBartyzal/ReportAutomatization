package com.reportplatform.form.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record FormFieldDefinition(
        @JsonProperty("fieldKey") @JsonAlias({"fieldKey", "field_key", "name", "key"}) String fieldKey,
        @JsonProperty("fieldType") @JsonAlias({"fieldType", "field_type", "type"}) String fieldType,
        @JsonAlias({"label", "title"}) String label,
        String section,
        @JsonAlias({"sectionDescription", "section_description"}) String sectionDescription,
        @JsonAlias({"sortOrder", "sort_order", "order"}) int sortOrder,
        boolean required,
        Map<String, Object> properties
) {
}
