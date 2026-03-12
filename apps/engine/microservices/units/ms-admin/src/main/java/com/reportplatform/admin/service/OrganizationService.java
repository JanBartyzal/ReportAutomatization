package com.reportplatform.admin.service;

import com.reportplatform.admin.model.dto.OrganizationDTO;
import com.reportplatform.admin.model.dto.CreateOrganizationRequest;
import com.reportplatform.admin.model.entity.OrganizationEntity;
import com.reportplatform.admin.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationDTO> getAllOrganizations() {
        List<OrganizationEntity> rootOrgs = organizationRepository.findTopLevelOrganizations();
        return rootOrgs.stream()
                .map(this::toDTOWithChildren)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrganizationDTO getOrganization(UUID id) {
        OrganizationEntity org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        return toDTO(org);
    }

    public OrganizationDTO createOrganization(CreateOrganizationRequest request) {
        // Validate parent exists if provided
        if (request.getParentId() != null) {
            organizationRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent organization not found"));
        }

        OrganizationEntity org = new OrganizationEntity();
        org.setName(request.getName());
        org.setCode(generateCode(request.getName()));
        org.setType(OrganizationEntity.OrganizationType.valueOf(request.getType().toUpperCase()));

        if (request.getParentId() != null) {
            OrganizationEntity parent = organizationRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent not found"));
            org.setParent(parent);
        }

        OrganizationEntity saved = organizationRepository.save(org);
        logger.info("Created organization: {} ({})", saved.getName(), saved.getId());
        return toDTO(saved);
    }

    public OrganizationDTO updateOrganization(UUID id, CreateOrganizationRequest request) {
        OrganizationEntity org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));

        if (request.getName() != null) {
            org.setName(request.getName());
        }
        if (request.getType() != null) {
            org.setType(OrganizationEntity.OrganizationType.valueOf(request.getType().toUpperCase()));
        }

        OrganizationEntity saved = organizationRepository.save(org);
        logger.info("Updated organization: {} ({})", saved.getName(), saved.getId());
        return toDTO(saved);
    }

    public void deleteOrganization(UUID id) {
        OrganizationEntity org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));

        // Check for children
        if (organizationRepository.hasChildren(id)) {
            throw new IllegalStateException("Cannot delete organization with children");
        }

        organizationRepository.delete(org);
        logger.info("Deleted organization: {}", id);
    }

    private OrganizationDTO toDTO(OrganizationEntity entity) {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setType(OrganizationDTO.OrganizationType.valueOf(entity.getType().name()));
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getParent() != null) {
            dto.setParentId(entity.getParent().getId());
        }

        return dto;
    }

    private OrganizationDTO toDTOWithChildren(OrganizationEntity entity) {
        OrganizationDTO dto = toDTO(entity);

        List<OrganizationEntity> children = organizationRepository.findByParentId(entity.getId());
        if (!children.isEmpty()) {
            dto.setChildren(children.stream()
                    .map(this::toDTOWithChildren)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private String generateCode(String name) {
        String base = name.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .substring(0, Math.min(name.length(), 10));

        String code = base;
        int suffix = 1;
        while (organizationRepository.existsByCode(code)) {
            code = base + suffix++;
        }
        return code;
    }
}
