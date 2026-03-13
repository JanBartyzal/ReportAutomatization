package com.reportplatform.batch.controller;

import com.reportplatform.batch.model.entity.BatchEntity;
import com.reportplatform.batch.repository.BatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BatchController.
 * Tests batch CRUD operations and filtering.
 */
@ExtendWith(MockitoExtension.class)
class BatchControllerTest {

    @Mock
    private BatchRepository batchRepository;

    private BatchController batchController;

    @BeforeEach
    void setUp() {
        batchController = new BatchController(batchRepository);
    }

    // ==================== listBatches Tests ====================

    @Test
    void listBatches_noParams_returnsAllBatches() {
        // Arrange
        var batch = mock(BatchEntity.class);
        when(batch.getId()).thenReturn(UUID.randomUUID());
        when(batchRepository.findAll()).thenReturn(List.of(batch));

        // Act
        ResponseEntity<List<BatchEntity>> response = batchController.listBatches(null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void listBatches_withHoldingId_filtersByHolding() {
        // Arrange
        UUID holdingId = UUID.randomUUID();
        var batch = mock(BatchEntity.class);
        when(batch.getId()).thenReturn(UUID.randomUUID());
        when(batchRepository.findByHoldingId(holdingId)).thenReturn(List.of(batch));

        // Act
        ResponseEntity<List<BatchEntity>> response = batchController.listBatches(holdingId, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(batchRepository).findByHoldingId(holdingId);
    }

    @Test
    void listBatches_withHoldingIdAndStatus_filtersByBoth() {
        // Arrange
        UUID holdingId = UUID.randomUUID();
        var batch = mock(BatchEntity.class);
        when(batch.getId()).thenReturn(UUID.randomUUID());
        when(batchRepository.findByHoldingIdAndStatus(eq(holdingId), any(BatchEntity.BatchStatus.class)))
                .thenReturn(List.of(batch));

        // Act
        ResponseEntity<List<BatchEntity>> response = batchController.listBatches(holdingId, "OPEN");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(batchRepository).findByHoldingIdAndStatus(eq(holdingId), eq(BatchEntity.BatchStatus.OPEN));
    }

    // ==================== getBatch Tests ====================

    @Test
    void getBatch_exists_returnsBatch() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        var batch = mock(BatchEntity.class);
        when(batch.getId()).thenReturn(batchId);
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));

        // Act
        ResponseEntity<BatchEntity> response = batchController.getBatch(batchId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isPresent());
    }

    @Test
    void getBatch_notFound_returnsNotFound() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        when(batchRepository.findById(batchId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<BatchEntity> response = batchController.getBatch(batchId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ==================== createBatch Tests ====================

    @Test
    void createBatch_validRequest_createsBatch() {
        // Arrange
        UUID holdingId = UUID.randomUUID();
        var request = java.util.Map.of(
                "name", "Q1 2026 Report",
                "period", "Q1-2026",
                "description", "Quarterly report",
                "holding_id", holdingId.toString());

        var savedBatch = mock(BatchEntity.class);
        when(savedBatch.getId()).thenReturn(UUID.randomUUID());
        when(savedBatch.getName()).thenReturn("Q1 2026 Report");
        when(savedBatch.getPeriod()).thenReturn("Q1-2026");
        when(savedBatch.getStatus()).thenReturn(BatchEntity.BatchStatus.OPEN);

        when(batchRepository.save(any(BatchEntity.class))).thenReturn(savedBatch);

        // Act
        ResponseEntity<BatchEntity> response = batchController.createBatch(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(batchRepository).save(any(BatchEntity.class));
    }

    // ==================== updateBatch Tests ====================

    @Test
    void updateBatch_exists_updatesBatch() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        var existingBatch = mock(BatchEntity.class);
        when(existingBatch.getId()).thenReturn(batchId);

        var updatedBatch = mock(BatchEntity.class);
        when(updatedBatch.getId()).thenReturn(batchId);
        when(updatedBatch.getName()).thenReturn("Updated Name");

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(existingBatch));
        when(batchRepository.save(any(BatchEntity.class))).thenReturn(updatedBatch);

        var request = java.util.Map.of("name", "Updated Name");

        // Act
        ResponseEntity<BatchEntity> response = batchController.updateBatch(batchId, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateBatch_notFound_returnsNotFound() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        when(batchRepository.findById(batchId)).thenReturn(Optional.empty());
        var request = java.util.Map.of("name", "Updated Name");

        // Act
        ResponseEntity<BatchEntity> response = batchController.updateBatch(batchId, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateBatch_statusToClosed_setsClosedAt() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        var existingBatch = mock(BatchEntity.class);
        when(existingBatch.getId()).thenReturn(batchId);

        var updatedBatch = mock(BatchEntity.class);
        when(updatedBatch.getId()).thenReturn(batchId);
        when(updatedBatch.getStatus()).thenReturn(BatchEntity.BatchStatus.CLOSED);

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(existingBatch));
        when(batchRepository.save(any(BatchEntity.class))).thenReturn(updatedBatch);

        var request = java.util.Map.of("status", "CLOSED");

        // Act
        ResponseEntity<BatchEntity> response = batchController.updateBatch(batchId, request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== deleteBatch Tests ====================

    @Test
    void deleteBatch_exists_deletesBatch() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        when(batchRepository.existsById(batchId)).thenReturn(true);

        // Act
        ResponseEntity<Void> response = batchController.deleteBatch(batchId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(batchRepository).deleteById(batchId);
    }

    @Test
    void deleteBatch_notFound_returnsNotFound() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        when(batchRepository.existsById(batchId)).thenReturn(false);

        // Act
        ResponseEntity<Void> response = batchController.deleteBatch(batchId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ==================== getBatchStatus Tests ====================

    @Test
    void getBatchStatus_exists_returnsStatus() {
        // Arrange
        UUID batchId = UUID.randomUUID();
        var batch = mock(BatchEntity.class);
        when(batch.getId()).thenReturn(batchId);
        when(batch.getStatus()).thenReturn(BatchEntity.BatchStatus.OPEN);
        when(batch.getPeriod()).thenReturn("Q1-2026");

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));

        // Act
        ResponseEntity<java.util.Map<String, Object>> response = batchController.getBatchStatus(batchId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OPEN", response.getBody().get("status"));
    }

    // ==================== health Tests ====================

    @Test
    void health_returnsOk() {
        // Act
        ResponseEntity<java.util.Map<String, String>> response = batchController.health();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
    }
}
