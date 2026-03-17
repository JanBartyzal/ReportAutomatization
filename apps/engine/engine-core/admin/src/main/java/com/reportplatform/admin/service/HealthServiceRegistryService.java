package com.reportplatform.admin.service;

import com.reportplatform.admin.model.dto.CreateHealthServiceRequest;
import com.reportplatform.admin.model.dto.HealthServiceRegistryDTO;
import com.reportplatform.admin.model.entity.HealthServiceEntity;
import com.reportplatform.admin.repository.HealthServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * CRUD service for managing health service registry entries.
 */
@Service
public class HealthServiceRegistryService {

    private final HealthServiceRepository repository;

    public HealthServiceRegistryService(HealthServiceRepository repository) {
        this.repository = repository;
    }

    public List<HealthServiceRegistryDTO> listAll() {
        return repository.findAllByOrderBySortOrder().stream()
                .map(this::toDTO)
                .toList();
    }

    public List<HealthServiceEntity> getEnabledServices() {
        return repository.findAllByEnabledTrueOrderBySortOrder();
    }

    public HealthServiceRegistryDTO create(CreateHealthServiceRequest request) {
        var entity = new HealthServiceEntity(
                request.serviceId(),
                request.displayName(),
                request.healthUrl(),
                request.sortOrder()
        );
        entity.setEnabled(request.enabled());
        return toDTO(repository.save(entity));
    }

    public HealthServiceRegistryDTO update(UUID id, CreateHealthServiceRequest request) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Health service not found: " + id));
        entity.setServiceId(request.serviceId());
        entity.setDisplayName(request.displayName());
        entity.setHealthUrl(request.healthUrl());
        entity.setEnabled(request.enabled());
        entity.setSortOrder(request.sortOrder());
        return toDTO(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private HealthServiceRegistryDTO toDTO(HealthServiceEntity entity) {
        return new HealthServiceRegistryDTO(
                entity.getId(),
                entity.getServiceId(),
                entity.getDisplayName(),
                entity.getHealthUrl(),
                entity.isEnabled(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
