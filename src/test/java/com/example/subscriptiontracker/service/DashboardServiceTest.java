package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.api.SubscriptionMapper;
import com.example.subscriptiontracker.domain.BillingPeriod;
import com.example.subscriptiontracker.domain.Subscription;
import com.example.subscriptiontracker.domain.SubscriptionCategory;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.domain.UserRole;
import com.example.subscriptiontracker.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private SubscriptionRepository repository;

    @Mock
    private CurrentUserService currentUserService;

    private DashboardService service;
    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User(UUID.randomUUID(), "user-a@example.com", "hash", "User A", UserRole.USER, true);
        org.mockito.Mockito.lenient().when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
        service = new DashboardService(repository, new SubscriptionMapper(), currentUserService);
    }

    @Test
    void calculatesMonthlyAndYearlyCostsWithoutMixingCurrencies() {
        Subscription usdMonthly = subscription("12.00", "USD", BillingPeriod.MONTHLY, SubscriptionStatus.ACTIVE);
        Subscription usdYearly = subscription("120.00", "USD", BillingPeriod.YEARLY, SubscriptionStatus.ACTIVE);
        Subscription eurMonthly = subscription("9.99", "EUR", BillingPeriod.MONTHLY, SubscriptionStatus.ACTIVE);
        Subscription cancelled = subscription("100.00", "USD", BillingPeriod.MONTHLY, SubscriptionStatus.CANCELLED);
        when(repository.findByUserIdAndStatus(currentUser.getId(), SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(usdMonthly, usdYearly, eurMonthly));
        when(repository.findByUserIdAndStatusAndNextPaymentDateBetweenOrderByNextPaymentDateAsc(
                eq(currentUser.getId()), eq(SubscriptionStatus.ACTIVE), any(), any())).thenReturn(List.of());

        var dashboard = service.getDashboard(30);

        assertThat(dashboard.activeSubscriptions()).isEqualTo(3);
        assertThat(dashboard.monthlyCost()).containsEntry("USD", new BigDecimal("22.00"));
        assertThat(dashboard.monthlyCost()).containsEntry("EUR", new BigDecimal("9.99"));
        assertThat(dashboard.yearlyCost()).containsEntry("USD", new BigDecimal("264.00"));
        assertThat(dashboard.yearlyCost()).containsEntry("EUR", new BigDecimal("119.88"));
        assertThat(dashboard.monthlyCost().values()).doesNotContain(cancelled.getPrice());
    }

    @Test
    void yearlyPriceIsConvertedToMonthlyEquivalent() {
        Subscription yearly = subscription("119.88", "USD", BillingPeriod.YEARLY, SubscriptionStatus.ACTIVE);

        assertThat(service.monthlyEquivalent(yearly).setScale(2)).isEqualByComparingTo("9.99");
        assertThat(service.yearlyEquivalent(yearly)).isEqualByComparingTo("119.88");
    }

    @Test
    void dashboardQueriesAreIsolatedForTwoUsers() {
        User userA = currentUser;
        User userB = new User(UUID.randomUUID(), "user-b@example.com", "hash", "User B", UserRole.USER, true);
        Subscription a1 = subscription("10.00", "USD", BillingPeriod.MONTHLY, SubscriptionStatus.ACTIVE);
        Subscription a2 = subscription("20.00", "USD", BillingPeriod.MONTHLY, SubscriptionStatus.ACTIVE);
        when(repository.findByUserIdAndStatus(userA.getId(), SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(a1, a2));
        when(repository.findByUserIdAndStatusAndNextPaymentDateBetweenOrderByNextPaymentDateAsc(
                eq(userA.getId()), eq(SubscriptionStatus.ACTIVE), any(), any())).thenReturn(List.of());

        var dashboardA = service.getDashboard(30);

        when(currentUserService.requireCurrentUser()).thenReturn(userB);
        Subscription b1 = new Subscription(UUID.randomUUID(), userB, "B1", null, new BigDecimal("1.00"), "EUR",
                BillingPeriod.MONTHLY, LocalDate.now(), LocalDate.now().plusDays(1),
                SubscriptionCategory.OTHER, SubscriptionStatus.ACTIVE);
        Subscription b2 = new Subscription(UUID.randomUUID(), userB, "B2", null, new BigDecimal("2.00"), "EUR",
                BillingPeriod.MONTHLY, LocalDate.now(), LocalDate.now().plusDays(2),
                SubscriptionCategory.OTHER, SubscriptionStatus.ACTIVE);
        Subscription b3 = new Subscription(UUID.randomUUID(), userB, "B3", null, new BigDecimal("3.00"), "EUR",
                BillingPeriod.MONTHLY, LocalDate.now(), LocalDate.now().plusDays(3),
                SubscriptionCategory.OTHER, SubscriptionStatus.ACTIVE);
        when(repository.findByUserIdAndStatus(userB.getId(), SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(b1, b2, b3));
        when(repository.findByUserIdAndStatusAndNextPaymentDateBetweenOrderByNextPaymentDateAsc(
                eq(userB.getId()), eq(SubscriptionStatus.ACTIVE), any(), any())).thenReturn(List.of(b1, b2, b3));

        var dashboardB = service.getDashboard(30);

        assertThat(dashboardA.activeSubscriptions()).isEqualTo(2);
        assertThat(dashboardA.monthlyCost()).containsOnlyKeys("USD");
        assertThat(dashboardB.activeSubscriptions()).isEqualTo(3);
        assertThat(dashboardB.monthlyCost()).containsOnlyKeys("EUR");
        assertThat(dashboardB.upcomingPayments()).hasSize(3);
    }

    private Subscription subscription(String price, String currency, BillingPeriod period,
                                      SubscriptionStatus status) {
        return new Subscription(UUID.randomUUID(), currentUser, "Service", null, new BigDecimal(price), currency,
                period, LocalDate.now().minusMonths(1), LocalDate.now().plusDays(5),
                SubscriptionCategory.OTHER, status);
    }
}
