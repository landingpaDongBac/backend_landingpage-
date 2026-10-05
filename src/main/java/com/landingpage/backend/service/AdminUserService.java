package com.landingpage.backend.service;

import com.landingpage.backend.api.dto.AdminUserCreateRequest;
import com.landingpage.backend.api.dto.AdminUserResponse;
import com.landingpage.backend.api.dto.AdminUserUpdateRequest;
import com.landingpage.backend.api.dto.PermissionResponse;
import com.landingpage.backend.domain.Role;
import com.landingpage.backend.domain.User;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.exception.ConflictException;
import com.landingpage.backend.exception.ResourceNotFoundException;
import com.landingpage.backend.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> list(String query, Role role, Boolean enabled, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Invalid pagination");
        Specification<User> spec = (root, criteriaQuery, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("email")), pattern),
                        builder.like(builder.lower(root.get("displayName")), pattern)));
            }
            if (role != null) {
                criteriaQuery.distinct(true);
                predicates.add(builder.equal(root.join("roles"), role));
            }
            if (enabled != null) predicates.add(builder.equal(root.get("enabled"), enabled));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return repository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "email")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID id) { return toResponse(find(id)); }

    @Transactional
    public AdminUserResponse create(AdminUserCreateRequest request, String actor) {
        String email = normalize(request.email());
        if (repository.existsByEmailIgnoreCase(email)) throw new ConflictException("USER_EMAIL_EXISTS", "Email already exists");
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(clean(request.displayName()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordChangedAt(Instant.now());
        user.setEnabled(true);
        user.setRoles(new HashSet<>(request.roles()));
        User saved = repository.save(user);
        auditLogService.record(actor, "USER_CREATED", "USER", saved.getId(), "Administrative account created");
        return toResponse(saved);
    }

    @Transactional
    public AdminUserResponse update(UUID id, AdminUserUpdateRequest request, String actor) {
        User user = find(id);
        String email = normalize(request.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("USER_EMAIL_EXISTS", "Email already exists");
        }
        user.setEmail(email);
        user.setDisplayName(clean(request.displayName()));
        User saved = repository.save(user);
        refreshTokenService.revokeAll(id);
        auditLogService.record(actor, "USER_UPDATED", "USER", id, "Administrative account updated");
        return toResponse(saved);
    }

    @Transactional
    public AdminUserResponse setStatus(UUID id, boolean enabled, boolean unlock, String actor) {
        User user = find(id);
        if (!enabled && user.isEnabled()) ensureNotLastAdmin(user);
        user.setEnabled(enabled);
        if (unlock) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }
        User saved = repository.save(user);
        if (!enabled) refreshTokenService.revokeAll(id);
        auditLogService.record(actor, "USER_STATUS_UPDATED", "USER", id,
                enabled ? "Account activated" : "Account deactivated");
        return toResponse(saved);
    }

    @Transactional
    public AdminUserResponse setRoles(UUID id, Set<Role> roles, String actor) {
        User user = find(id);
        if (user.isEnabled() && user.getRoles().contains(Role.ADMIN) && !roles.contains(Role.ADMIN)) {
            ensureNotLastAdmin(user);
        }
        user.setRoles(new HashSet<>(roles));
        User saved = repository.save(user);
        refreshTokenService.revokeAll(id);
        auditLogService.record(actor, "USER_ROLES_UPDATED", "USER", id, "Account roles updated");
        return toResponse(saved);
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword, String actor) {
        User user = find(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        repository.save(user);
        refreshTokenService.revokeAll(id);
        auditLogService.record(actor, "USER_PASSWORD_RESET", "USER", id, "Account password reset by administrator");
    }

    public List<PermissionResponse> permissions() {
        return List.of(
                new PermissionResponse(Role.ADMIN, List.of("LEADS_MANAGE", "CONTENT_MANAGE", "CONTENT_PUBLISH",
                        "MEDIA_MANAGE", "SECTIONS_MANAGE", "USERS_MANAGE", "SETTINGS_MANAGE", "AUDIT_READ")),
                new PermissionResponse(Role.EDITOR, List.of("CONTENT_DRAFT_MANAGE", "CONTENT_REVISIONS_READ",
                        "CONTENT_REVISIONS_RESTORE", "MEDIA_MANAGE", "SECTIONS_READ")));
    }

    private void ensureNotLastAdmin(User user) {
        if (user.getRoles().contains(Role.ADMIN)
                && repository.countDistinctByEnabledTrueAndRolesContaining(Role.ADMIN) <= 1) {
            throw new ConflictException("LAST_ACTIVE_ADMIN", "The last active ADMIN account cannot be disabled or demoted");
        }
    }

    private User find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AdminUserResponse toResponse(User user) {
        boolean locked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.isEnabled(), locked,
                user.getLockedUntil(), Set.copyOf(user.getRoles()), user.getLastLoginAt(), user.getPasswordChangedAt(),
                user.getCreatedAt(), user.getUpdatedAt());
    }

    private String normalize(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
