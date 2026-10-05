package com.landingpage.backend.service;

import com.landingpage.backend.api.dto.AuditLogResponse;
import com.landingpage.backend.domain.AuditLog;
import com.landingpage.backend.repository.AuditLogRepository;
import com.landingpage.backend.repository.UserRepository;
import com.landingpage.backend.exception.BadRequestException;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository repository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actorEmail, String action, String entityType, Object entityId, String description) {
        AuditLog log = new AuditLog();
        log.setActorEmail(normalize(actorEmail));
        if (actorEmail != null) userRepository.findByEmailIgnoreCase(actorEmail).ifPresent(log::setActor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId == null ? null : entityId.toString());
        log.setDescription(description == null ? null : description.substring(0, Math.min(description.length(), 1000)));
        log.setIpAddress(currentIp());
        repository.save(log);
    }

    public void recordCurrent(String action, String entityType, Object entityId, String description) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal()) ? null : authentication.getName();
        record(email, action, entityType, entityId, description);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(UUID actorId, String action, String entityType,
                                         Instant createdFrom, Instant createdTo, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Invalid pagination");
        Specification<AuditLog> spec = (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (actorId != null) predicates.add(builder.equal(root.get("actor").get("id"), actorId));
            if (action != null && !action.isBlank()) predicates.add(builder.equal(root.get("action"), action.trim()));
            if (entityType != null && !entityType.isBlank()) predicates.add(builder.equal(root.get("entityType"), entityType.trim()));
            if (createdFrom != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            if (createdTo != null) predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return repository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getActor() == null ? null : log.getActor().getId(),
                log.getActorEmail(), log.getAction(), log.getEntityType(), log.getEntityId(),
                log.getDescription(), log.getCreatedAt(), log.getIpAddress());
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
