package com.reportplatform.dash.model.dto;

import java.util.List;
import java.util.Map;

public record RawSqlResponse(
        List<String> columns,
        List<List<Object>> rows,
        int totalRows
) {}