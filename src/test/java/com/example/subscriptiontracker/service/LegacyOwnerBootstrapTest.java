package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.domain.UserRole;
import com.example.subscriptiontracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyOwnerBootstrapTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    @Mock
    private ApplicationArguments arguments;

    @Test
    void activatedLegacyOwnerDoesNotRequireBootstrapPassword() {
        User activatedOwner = new User(User.LEGACY_OWNER_ID, "owner@example.com", "hash",
                "Owner", UserRole.ADMIN, true);
        when(repository.findById(User.LEGACY_OWNER_ID)).thenReturn(Optional.of(activatedOwner));

        new LegacyOwnerBootstrap(repository, passwordEncoder, environment).run(arguments);

        verify(environment, never()).getProperty("app.bootstrap.initial-admin-password", "");
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void inactiveLegacyOwnerStillRequiresCompleteBootstrapCredentials() {
        User inactiveOwner = new User(User.LEGACY_OWNER_ID, "legacy-owner@invalid.local", "!",
                "Legacy owner", UserRole.ADMIN, false);
        when(repository.findById(User.LEGACY_OWNER_ID)).thenReturn(Optional.of(inactiveOwner));
        when(environment.getProperty("app.bootstrap.initial-admin-email", ""))
                .thenReturn("owner@example.com");
        when(environment.getProperty("app.bootstrap.initial-admin-password", ""))
                .thenReturn("");
        when(environment.getProperty("app.bootstrap.initial-admin-display-name", "Legacy owner"))
                .thenReturn("Owner");

        LegacyOwnerBootstrap bootstrap = new LegacyOwnerBootstrap(repository, passwordEncoder, environment);

        assertThatThrownBy(() -> bootstrap.run(arguments))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires both email and a password");
    }
}
