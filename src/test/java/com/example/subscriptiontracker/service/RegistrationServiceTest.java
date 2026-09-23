package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.exception.DuplicateEmailException;
import com.example.subscriptiontracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository repository;

    private BCryptPasswordEncoder passwordEncoder;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new RegistrationService(repository, passwordEncoder);
    }

    @Test
    void validSignupCreatesUserWithNormalizedEmailAndHashedPassword() {
        when(repository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.register("  Person@Example.COM ", "correct-horse", "  Person  ");

        assertThat(user.getEmail()).isEqualTo("person@example.com");
        assertThat(user.getDisplayName()).isEqualTo("Person");
        assertThat(user.getPasswordHash()).isNotEqualTo("correct-horse");
        assertThat(passwordEncoder.matches("correct-horse", user.getPasswordHash())).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void duplicateEmailIsRejectedBeforePasswordIsStored() {
        when(repository.existsByEmail("person@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("Person@Example.com", "correct-horse", "Person"))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("An account with this email already exists.");
        verify(repository, never()).saveAndFlush(any());
    }
}
