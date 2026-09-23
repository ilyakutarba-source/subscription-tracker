package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LegacyOwnerBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyOwnerBootstrap.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public LegacyOwnerBootstrap(UserRepository repository, PasswordEncoder passwordEncoder,
                                Environment environment) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String initialEmail = environment.getProperty("app.bootstrap.initial-admin-email", "");
        String initialPassword = environment.getProperty("app.bootstrap.initial-admin-password", "");
        String initialDisplayName = environment.getProperty(
                "app.bootstrap.initial-admin-display-name", "Legacy owner");
        boolean emailPresent = !initialEmail.isBlank();
        boolean passwordPresent = !initialPassword.isBlank();
        if (!emailPresent && !passwordPresent) {
            return;
        }
        if (!emailPresent || !passwordPresent || initialPassword.length() < 8 || initialPassword.length() > 72) {
            throw new IllegalStateException(
                    "Initial admin bootstrap requires both email and a password between 8 and 72 characters");
        }

        User legacyOwner = repository.findById(User.LEGACY_OWNER_ID).orElse(null);
        if (legacyOwner == null || legacyOwner.isEnabled()) {
            LOGGER.info("Initial admin bootstrap skipped: legacy owner is already activated or absent");
            return;
        }

        String normalizedEmail = RegistrationService.normalizeEmail(initialEmail);
        if (repository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Initial admin email is already used by another account");
        }
        legacyOwner.activateLegacyOwner(normalizedEmail, passwordEncoder.encode(initialPassword),
                initialDisplayName.isBlank() ? "Legacy owner" : initialDisplayName.trim());
        LOGGER.info("Legacy owner activated: userId={}", legacyOwner.getId());
    }
}
