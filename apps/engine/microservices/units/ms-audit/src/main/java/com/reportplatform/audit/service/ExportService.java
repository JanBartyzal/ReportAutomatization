package com.reportplatform.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.reportplatform.audit.model.AuditLogEntity;
import com.reportplatform.audit.model.dto.AuditFilterRequest;
import com.reportplatform.audit.repository.AuditLogRepository;
import com.reportplatform.audit.repository.AuditLogSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public ExportService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public StreamingResponseBody exportLogs(UUID orgId, AuditFilterRequest filter, String format) {
        return outputStream -> {
            var spec = AuditLogSpecification.withFilters(
                    orgId,
                    filter.userId(),
                    filter.action(),
                    filter.entityType(),
                    filter.entityId(),
                    filter.dateFrom(),
                    filter.dateTo()
            );

            var logs = auditLogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

            if ("csv".equalsIgnoreCase(format)) {
                exportToCsv(logs, outputStream);
            } else {
                exportToJson(logs, outputStream);
            }
        };
    }

    private void exportToCsv(Iterable<AuditLogEntity> logs, OutputStream outputStream) {
        try (var writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write("id,org_id,user_id,action,entity_type,entity_id,details,ip_address,created_at\n");
            for (AuditLogEntity entity : logs) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                        entity.getId(),
                        entity.getOrgId(),
                        escapeCsv(entity.getUserId()),
                        escapeCsv(entity.getAction()),
                        escapeCsv(entity.getEntityType()),
                        entity.getEntityId() != null ? entity.getEntityId() : "",
                        escapeCsv(entity.getDetails() != null ? entity.getDetails() : ""),
                        escapeCsv(entity.getIpAddress() != null ? entity.getIpAddress() : ""),
                        entity.getCreatedAt()
                ));
            }
            writer.flush();
        } catch (Exception e) {
            log.error("Error exporting audit logs to CSV", e);
            throw new RuntimeException("Export failed", e);
        }
    }

    private void exportToJson(Iterable<AuditLogEntity> logs, OutputStream outputStream) {
        try (var generator = objectMapper.getFactory().createGenerator(outputStream)) {
            generator.writeStartArray();
            for (AuditLogEntity entity : logs) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", entity.getId().toString());
                row.put("orgId", entity.getOrgId().toString());
                row.put("userId", entity.getUserId());
                row.put("action", entity.getAction());
                row.put("entityType", entity.getEntityType());
                row.put("entityId", entity.getEntityId() != null ? entity.getEntityId().toString() : null);
                row.put("details", entity.getDetails());
                row.put("ipAddress", entity.getIpAddress());
                row.put("createdAt", entity.getCreatedAt().toString());
                generator.writeObject(row);
            }
            generator.writeEndArray();
            generator.flush();
        } catch (Exception e) {
            log.error("Error exporting audit logs to JSON", e);
            throw new RuntimeException("Export failed", e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
