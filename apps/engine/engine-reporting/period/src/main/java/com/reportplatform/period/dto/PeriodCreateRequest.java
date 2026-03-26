package com.reportplatform.period.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PeriodCreateRequest(
        @NotBlank String name,
        @JsonProperty("type") @JsonAlias({"periodType", "type", "period_type"}) String periodType,
        @JsonProperty("period_code") @JsonAlias({"periodCode", "period_code"}) String periodCode,
        @JsonProperty("start_date") @JsonAlias({"startDate", "start_date"})
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonProperty("end_date") @JsonAlias({"endDate", "end_date"})
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        @JsonProperty("submission_deadline") @JsonAlias({"submissionDeadline", "submission_deadline"})
        @JsonDeserialize(using = FlexibleInstantDeserializer.class) Instant submissionDeadline,
        @JsonProperty("review_deadline") @JsonAlias({"reviewDeadline", "review_deadline"})
        @JsonDeserialize(using = FlexibleInstantDeserializer.class) Instant reviewDeadline,
        @JsonProperty("holding_id") @JsonAlias({"holdingId", "holding_id"}) String holdingId,
        @JsonProperty("org_ids") @JsonAlias({"orgIds", "org_ids"}) List<String> orgIds
) {}
