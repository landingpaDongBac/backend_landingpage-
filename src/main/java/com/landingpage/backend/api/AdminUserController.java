package com.landingpage.backend.api;

import com.landingpage.backend.api.dto.AdminUserCreateRequest;
import com.landingpage.backend.api.dto.AdminUserResponse;
import com.landingpage.backend.api.dto.AdminUserUpdateRequest;
import com.landingpage.backend.api.dto.PasswordResetRequest;
import com.landingpage.backend.api.dto.PermissionResponse;
import com.landingpage.backend.api.dto.UserRolesRequest;
import com.landingpage.backend.api.dto.UserStatusRequest;
import com.landingpage.backend.domain.Role;
import com.landingpage.backend.security.UserPrincipal;
import com.landingpage.backend.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "ADMIN-only account, role, status, password-reset, and permission management")
public class AdminUserController {
    private final AdminUserService service;

    @GetMapping("/users")
    public Page<AdminUserResponse> list(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) Role role,
                                        @RequestParam(required = false) Boolean enabled,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return service.list(q, role, enabled, page, size);
    }

    @GetMapping("/users/{id}")
    public AdminUserResponse get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping("/users")
    public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody AdminUserCreateRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal.email()));
    }

    @PutMapping("/users/{id}")
    public AdminUserResponse update(@PathVariable UUID id, @Valid @RequestBody AdminUserUpdateRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return service.update(id, request, principal.email());
    }

    @PatchMapping("/users/{id}/status")
    public AdminUserResponse status(@PathVariable UUID id, @RequestBody UserStatusRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return service.setStatus(id, request.enabled(), request.unlock(), principal.email());
    }

    @PatchMapping("/users/{id}/roles")
    public AdminUserResponse roles(@PathVariable UUID id, @Valid @RequestBody UserRolesRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return service.setRoles(id, request.roles(), principal.email());
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id,
                                              @Valid @RequestBody PasswordResetRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        service.resetPassword(id, request.newPassword(), principal.email());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissions")
    public List<PermissionResponse> permissions() { return service.permissions(); }
}
