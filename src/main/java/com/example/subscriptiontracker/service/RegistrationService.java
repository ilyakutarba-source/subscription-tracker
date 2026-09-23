package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.domain.UserRole;
import com.example.subscriptiontracker.exception.DuplicateEmailException;
import com.example.subscriptiontracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class RegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String email, String rawPassword, String displayName) {
        String normalizedEmail = normalizeEmail(email);
        if (repository.existsByEmail(normalizedEmail)) {
            LOGGER.warn("Registration rejected: duplicate normalized email");
            throw new DuplicateEmailException();
        }

        User user = new User(
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                displayName.trim(),
                UserRole.USER,
                true
        );
        try {
            User saved = repository.saveAndFlush(user);
            LOGGER.info("User registered: userId={}", saved.getId());
            return saved;
        } catch (DataIntegrityViolationException exception) {
            LOGGER.warn("Registration rejected: duplicate normalized email");
            throw new DuplicateEmailException();
        }
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
