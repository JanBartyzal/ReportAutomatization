package com.reportplatform.dash.model.dto;

import java.util.List;
import java.util.Map;

public record DashboardDataResponse(
        List<Map<String, Object>> data,
        Map<String, Object> metadata
) {}
