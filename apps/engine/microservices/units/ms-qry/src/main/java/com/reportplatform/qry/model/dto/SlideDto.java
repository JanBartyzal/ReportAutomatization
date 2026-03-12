package com.reportplatform.qry.model.dto;

import java.util.List;

public record SlideDto(
        int slideIndex,
        String title,
        List<Object> texts,
        List<Object> tables,
        String imageUrl,
        String notes
) {
}
