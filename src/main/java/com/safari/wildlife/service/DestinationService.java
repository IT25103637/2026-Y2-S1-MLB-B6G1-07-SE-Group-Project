package com.safari.wildlife.service;

import com.safari.wildlife.dto.DestinationDto;
import com.safari.wildlife.entity.Destination;
import com.safari.wildlife.exception.BadRequestException;
import com.safari.wildlife.exception.ResourceNotFoundException;
import com.safari.wildlife.repository.DestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final AuditService auditService;
    private final AuthService authService;

    @Autowired
    public DestinationService(DestinationRepository destinationRepository,
                              AuditService auditService,
                              AuthService authService) {
        this.destinationRepository = destinationRepository;
        this.auditService = auditService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<Destination> getAllDestinations() {
        return destinationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Destination> getActiveDestinations() {
        return destinationRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Destination getDestinationById(Long id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + id));
    }

    @Transactional
    public Destination createDestination(DestinationDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BadRequestException("Destination name is required.");
        }
        if (dto.getProvince() == null || dto.getProvince().trim().isEmpty()) {
            throw new BadRequestException("Province is required.");
        }

        // Duplicate check
        if (destinationRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new BadRequestException("A destination with the name '" + dto.getName().trim() + "' already exists.");
        }

        Destination destination = new Destination(
                dto.getName().trim(),
                dto.getProvince().trim(),
                dto.getDescription(),
                dto.getEntryRules(),
                dto.getOpeningTime() != null ? dto.getOpeningTime() : "06:00 AM",
                dto.getClosingTime() != null ? dto.getClosingTime() : "06:00 PM",
                dto.getImageUrl()
        );
        destination.setActive(dto.isActive());

        Destination saved = destinationRepository.save(destination);
        auditService.logAction(authService.getCurrentUser(), "DESTINATION_CREATE", "Destination",
                String.valueOf(saved.getId()), "Created destination: " + saved.getName(), "127.0.0.1");

        return saved;
    }

    @Transactional
    public Destination updateDestination(Long id, DestinationDto dto) {
        Destination destination = getDestinationById(id);

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BadRequestException("Destination name is required.");
        }

        // Duplicate check excluding self
        if (destinationRepository.existsByNameIgnoreCaseAndIdNot(dto.getName().trim(), id)) {
            throw new BadRequestException("Another destination with name '" + dto.getName().trim() + "' already exists.");
        }

        destination.setName(dto.getName().trim());
        destination.setProvince(dto.getProvince().trim());
        destination.setDescription(dto.getDescription());
        destination.setEntryRules(dto.getEntryRules());
        destination.setOpeningTime(dto.getOpeningTime());
        destination.setClosingTime(dto.getClosingTime());
        if (dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty()) {
            destination.setImageUrl(dto.getImageUrl().trim());
        }
        destination.setActive(dto.isActive());

        Destination updated = destinationRepository.save(destination);
        auditService.logAction(authService.getCurrentUser(), "DESTINATION_UPDATE", "Destination",
                String.valueOf(updated.getId()), "Updated destination: " + updated.getName(), "127.0.0.1");

        return updated;
    }

    @Transactional
    public void deleteOrDeactivateDestination(Long id) {
        Destination destination = getDestinationById(id);
        destination.setActive(false);
        destinationRepository.save(destination);

        auditService.logAction(authService.getCurrentUser(), "DESTINATION_DEACTIVATE", "Destination",
                String.valueOf(id), "Deactivated destination: " + destination.getName(), "127.0.0.1");
    }

    public Destination updateStatus(Long id, boolean active) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Destination not found with id: " + id));
        destination.setActive(active);
        return destinationRepository.save(destination);
    }
}
