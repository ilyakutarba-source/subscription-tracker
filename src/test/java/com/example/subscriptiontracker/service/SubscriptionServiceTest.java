package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.api.dto.SubscriptionRequest;
import com.example.subscriptiontracker.domain.BillingPeriod;
import com.example.subscriptiontracker.domain.Subscription;
import com.example.subscriptiontracker.domain.SubscriptionCategory;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.domain.UserRole;
import com.example.subscriptiontracker.exception.SubscriptionNotFoundException;
import com.example.subscriptiontracker.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository repository;

    @Mock
    private CurrentUserService currentUserService;

    private SubscriptionService service;
    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = user("user-a@example.com");
        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        service = new SubscriptionService(repository, currentUserService);
    }

    @Test
    void createsActiveSubscriptionAndNormalizesInput() {
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new SubscriptionRequest(
                "  ChatGPT  ", "  AI assistant  ", new BigDecimal("20.00"), "usd",
                BillingPeriod.MONTHLY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                SubscriptionCategory.SOFTWARE, null);

        Subscription created = service.create(request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getUser()).isSameAs(currentUser);
        assertThat(created.getName()).isEqualTo("ChatGPT");
        assertThat(created.getDescription()).isEqualTo("AI assistant");
        assertThat(created.getCurrency()).isEqualTo("USD");
        assertThat(created.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void listsOnlyCurrentUsersSubscriptions() {
        when(repository.findAllByUserIdOrderByCreatedAtDesc(currentUser.getId())).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();

        org.mockito.Mockito.verify(repository).findAllByUserIdOrderByCreatedAtDesc(currentUser.getId());
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).findAll();
    }

    @Test
    void listQueriesRemainIsolatedWhenCurrentUserChanges() {
        User userB = user("user-b@example.com");
        when(repository.findAllByUserIdOrderByCreatedAtDesc(currentUser.getId())).thenReturn(List.of());
        when(repository.findAllByUserIdOrderByCreatedAtDesc(userB.getId())).thenReturn(List.of());

        service.findAll();
        when(currentUserService.requireCurrentUser()).thenReturn(userB);
        service.findAll();

        org.mockito.Mockito.verify(repository).findAllByUserIdOrderByCreatedAtDesc(currentUser.getId());
        org.mockito.Mockito.verify(repository).findAllByUserIdOrderByCreatedAtDesc(userB.getId());
    }

    @Test
    void cannotReadAnotherUsersSubscription() {
        UUID foreignSubscriptionId = UUID.randomUUID();
        when(repository.findByIdAndUserId(foreignSubscriptionId, currentUser.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(foreignSubscriptionId))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void cannotUpdateCancelOrDeleteAnotherUsersSubscription() {
        UUID foreignSubscriptionId = UUID.randomUUID();
        when(repository.findByIdAndUserId(foreignSubscriptionId, currentUser.getId()))
                .thenReturn(Optional.empty());
        var request = new SubscriptionRequest(
                "ChatGPT", null, new BigDecimal("20.00"), "USD", BillingPeriod.MONTHLY,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), SubscriptionCategory.SOFTWARE, null);

        assertThatThrownBy(() -> service.update(foreignSubscriptionId, request))
                .isInstanceOf(SubscriptionNotFoundException.class);
        assertThatThrownBy(() -> service.cancel(foreignSubscriptionId))
                .isInstanceOf(SubscriptionNotFoundException.class);
        assertThatThrownBy(() -> service.delete(foreignSubscriptionId))
                .isInstanceOf(SubscriptionNotFoundException.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).delete(any());
    }

    private User user(String email) {
        return new User(UUID.randomUUID(), email, "hash", "User", UserRole.USER, true);
    }
}
