package com.reportplatform.qry.model.dto;

import java.util.List;
import java.util.UUID;

public record SlideDataResponse(
        UUID fileId,
        String filename,
        List<SlideDto> slides
) {
}
