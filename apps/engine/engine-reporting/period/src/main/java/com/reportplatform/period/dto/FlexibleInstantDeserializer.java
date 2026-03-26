package com.reportplatform.period.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Flexible deserializer for Instant that handles:
 * - ISO-8601 Instant format ("2026-03-31T00:00:00Z")
 * - Date-only format ("2026-03-31") → converted to start-of-day UTC
 * - Epoch millis (numeric)
 */
public class FlexibleInstantDeserializer extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }

        // Try ISO-8601 Instant
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
        }

        // Try date-only format
        try {
            LocalDate date = LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        // Try epoch millis
        try {
            long epochMillis = Long.parseLong(text);
            return Instant.ofEpochMilli(epochMillis);
        } catch (NumberFormatException ignored) {
        }

        throw new IOException("Cannot parse Instant from: " + text);
    }
}
