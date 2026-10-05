package com.landingpage.backend.security;

import com.landingpage.backend.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.time.Instant;
import java.util.UUID;

public record UserPrincipal(
        UUID id,
        String email,
        String password,
        boolean enabled,
        Instant lockedUntil,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    public UserPrincipal(UUID id, String email, String password, boolean enabled,
                         Collection<? extends GrantedAuthority> authorities) {
        this(id, email, password, enabled, null, authorities);
    }

    public static UserPrincipal from(User user) {
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.isEnabled(),
                user.getLockedUntil(), authorities);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || !lockedUntil.isAfter(Instant.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
