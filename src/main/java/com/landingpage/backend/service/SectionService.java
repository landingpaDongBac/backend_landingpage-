package com.landingpage.backend.service;

import com.landingpage.backend.api.dto.SectionResponse;
import com.landingpage.backend.api.dto.SectionOrderRequest;
import com.landingpage.backend.api.dto.SectionUpdateRequest;
import com.landingpage.backend.domain.Section;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.exception.ResourceNotFoundException;
import com.landingpage.backend.repository.SectionRepository;
import com.landingpage.backend.repository.ContentDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final ContentDocumentRepository contentDocumentRepository;

    @Autowired
    public SectionService(SectionRepository sectionRepository, ContentDocumentRepository contentDocumentRepository) {
        this.sectionRepository = sectionRepository;
        this.contentDocumentRepository = contentDocumentRepository;
    }

    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
        this.contentDocumentRepository = null;
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> list() {
        return sectionRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(section -> toResponseWithCount(section, count(section.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public SectionResponse get(UUID id) {
        Section section = find(id);
        return toResponseWithCount(section, count(id));
    }

    @Transactional
    public SectionResponse setVisibility(UUID id, boolean enabled) {
        Section section = find(id);
        section.setEnabled(enabled);
        Section saved = sectionRepository.save(section);
        return toResponseWithCount(saved, count(saved.getId()));
    }

    @Transactional
    public List<SectionResponse> reorder(SectionOrderRequest request) {
        List<Section> sections = sectionRepository.findAll();
        if (request.items().size() != sections.size()) {
            throw new BadRequestException("Section order request must contain every controlled section exactly once");
        }
        var ids = new HashSet<UUID>();
        var orders = new HashSet<Integer>();
        for (SectionOrderRequest.Item item : request.items()) {
            if (!ids.add(item.sectionId())) throw new BadRequestException("Duplicate section identifier");
            if (!orders.add(item.displayOrder())) throw new BadRequestException("Duplicate section display order");
        }
        Map<UUID, Integer> requested = request.items().stream()
                .collect(Collectors.toMap(SectionOrderRequest.Item::sectionId, SectionOrderRequest.Item::displayOrder));
        if (!requested.keySet().equals(sections.stream().map(Section::getId).collect(Collectors.toSet()))) {
            throw new BadRequestException("Section order contains an unknown or missing identifier");
        }
        sections.forEach(section -> section.setDisplayOrder(requested.get(section.getId())));
        sectionRepository.saveAll(sections);
        return list();
    }

    @Transactional
    public SectionResponse update(UUID id, SectionUpdateRequest request) {
        if (!request.configuration().isObject()) {
            throw new BadRequestException("Section configuration must be a JSON object");
        }
        if (request.configuration().toString().length() > 20_000) {
            throw new BadRequestException("Section configuration is too large");
        }
        Section section = find(id);
        section.setName(request.name().trim());
        section.setDescription(clean(request.description()));
        section.setConfigurationJson(request.configuration().deepCopy());
        Section saved = sectionRepository.save(section);
        return toResponseWithCount(saved, count(saved.getId()));
    }

    private Section find(UUID id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
    }

    public SectionResponse toResponse(Section section) {
        return new SectionResponse(section.getId(), section.getSectionKey(), section.getName(),
                section.getDisplayOrder(), section.isEnabled(), section.getUpdatedAt(),
                0L, section.getDescription(), copy(section.getConfigurationJson()));
    }

    public SectionResponse toResponseWithCount(Section section, long documentCount) {
        return new SectionResponse(section.getId(), section.getSectionKey(), section.getName(),
                section.getDisplayOrder(), section.isEnabled(), section.getUpdatedAt(), documentCount,
                section.getDescription(), copy(section.getConfigurationJson()));
    }

    private com.fasterxml.jackson.databind.JsonNode copy(com.fasterxml.jackson.databind.JsonNode value) {
        return value == null ? com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode() : value.deepCopy();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private long count(UUID sectionId) {
        return contentDocumentRepository == null ? 0L : contentDocumentRepository.countBySectionId(sectionId);
    }
}
