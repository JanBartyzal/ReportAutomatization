package com.reportplatform.form.dto;

import java.util.Map;

public record FormResponseUpdateRequest(
        Map<String, Object> data,
        boolean submit
) {
}
