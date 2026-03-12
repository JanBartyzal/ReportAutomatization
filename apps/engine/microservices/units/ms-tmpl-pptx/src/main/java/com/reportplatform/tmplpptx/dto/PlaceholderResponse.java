package com.reportplatform.tmplpptx.dto;

public record PlaceholderResponse(
    String key,
    String type,
    Integer slideIndex,
    String shapeName
) {}
