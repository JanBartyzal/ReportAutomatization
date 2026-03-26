package com.reportplatform.period.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;
import java.time.LocalDate;

public record CloneRequest(
        @JsonProperty("name") @JsonAlias({"newName", "new_name"}) String newName,
        @JsonProperty("period_code") @JsonAlias({"newPeriodCode", "new_period_code", "periodCode"}) String newPeriodCode,
        @JsonProperty("start_date") @JsonAlias({"newStartDate", "new_start_date", "startDate"})
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate newStartDate,
        @JsonProperty("end_date") @JsonAlias({"newEndDate", "new_end_date", "endDate"})
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate newEndDate,
        @JsonProperty("submission_deadline") @JsonAlias({"newSubmissionDeadline", "new_submission_deadline", "submissionDeadline"})
        @JsonDeserialize(using = FlexibleInstantDeserializer.class) Instant newSubmissionDeadline,
        @JsonProperty("review_deadline") @JsonAlias({"newReviewDeadline", "new_review_deadline", "reviewDeadline"})
        @JsonDeserialize(using = FlexibleInstantDeserializer.class) Instant newReviewDeadline
) {}
