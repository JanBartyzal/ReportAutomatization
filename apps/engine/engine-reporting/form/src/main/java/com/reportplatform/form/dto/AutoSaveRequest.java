package com.reportplatform.form.dto;

import java.util.Map;

public record AutoSaveRequest(
        Map<String, Object> data
) {
}
