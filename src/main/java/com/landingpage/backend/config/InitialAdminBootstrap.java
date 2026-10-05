package com.landingpage.backend.config;

import com.landingpage.backend.domain.Role;
import com.landingpage.backend.domain.User;
import com.landingpage.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class InitialAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-admin.email:}")
    private String initialAdminEmail;

    @Value("${app.initial-admin.password:}")
    private String initialAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (initialAdminEmail == null || initialAdminEmail.isBlank()
                || initialAdminPassword == null || initialAdminPassword.isBlank()) {
            log.warn("No users exist. Configure INITIAL_ADMIN_EMAIL and INITIAL_ADMIN_PASSWORD to bootstrap an admin.");
            return;
        }
        if (initialAdminPassword.length() < 12) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must contain at least 12 characters");
        }
        User admin = new User();
        admin.setEmail(initialAdminEmail.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(initialAdminPassword));
        admin.setPasswordChangedAt(Instant.now());
        admin.setEnabled(true);
        admin.setRoles(Set.of(Role.ADMIN));
        userRepository.save(admin);
        log.info("Initial admin account created for {}", admin.getEmail());
    }
}
