package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.domain.UserRole;
import com.example.subscriptiontracker.repository.SubscriptionRepository;
import com.example.subscriptiontracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CurrentUserService currentUserService;

    private AdminService service;
    private User currentAdmin;

    @BeforeEach
    void setUp() {
        currentAdmin = user(UUID.randomUUID(), "admin@example.com", UserRole.ADMIN, true);
        service = new AdminService(userRepository, subscriptionRepository, currentUserService);
    }

    @Test
    void overviewCombinesUserAndSubscriptionCounts() {
        when(userRepository.count()).thenReturn(4L);
        when(userRepository.countByEnabledTrue()).thenReturn(3L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);
        when(subscriptionRepository.count()).thenReturn(9L);

        assertThat(service.getOverview()).isEqualTo(new AdminOverview(4, 3, 1, 9));
    }

    @Test
    void usersIncludeOwnershipCountsAndCurrentAccountMarker() {
        User member = user(UUID.randomUUID(), "member@example.com", UserRole.USER, true);
        when(currentUserService.requireCurrentUser()).thenReturn(currentAdmin);
        when(userRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(member, currentAdmin));
        when(subscriptionRepository.countByUserId(member.getId())).thenReturn(2L);
        when(subscriptionRepository.countByUserId(currentAdmin.getId())).thenReturn(3L);

        List<AdminUserSummary> users = service.findUsers();

        assertThat(users).hasSize(2);
        assertThat(users.get(0).subscriptionCount()).isEqualTo(2);
        assertThat(users.get(0).currentAccount()).isFalse();
        assertThat(users.get(1).currentAccount()).isTrue();
    }

    @Test
    void adminCanChangeAnotherUsersRoleAndStatus() {
        User member = user(UUID.randomUUID(), "member@example.com", UserRole.USER, true);
        when(currentUserService.requireCurrentUser()).thenReturn(currentAdmin);
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));

        service.changeRole(member.getId(), UserRole.ADMIN);
        service.changeEnabled(member.getId(), false);

        assertThat(member.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(member.isEnabled()).isFalse();
    }

    @Test
    void adminCannotBlockOwnAccount() {
        when(currentUserService.requireCurrentUser()).thenReturn(currentAdmin);
        when(userRepository.findById(currentAdmin.getId())).thenReturn(Optional.of(currentAdmin));

        assertThatThrownBy(() -> service.changeEnabled(currentAdmin.getId(), false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You cannot block your own account.");
    }

    private User user(UUID id, String email, UserRole role, boolean enabled) {
        return new User(id, email, "hash", email.substring(0, email.indexOf('@')), role, enabled);
    }
}
