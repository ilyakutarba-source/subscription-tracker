package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.api.SubscriptionMapper;
import com.example.subscriptiontracker.domain.BillingPeriod;
import com.example.subscriptiontracker.domain.Subscription;
import com.example.subscriptiontracker.domain.SubscriptionCategory;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
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

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(repository, new SubscriptionMapper());
    }

    @Test
    void calculatesMonthlyAndYearlyCostsWithoutMixingCurrencies() {
        Subscription usdMonthly = subscription("12.00", "USD", BillingPeriod.MONTHLY, SubscriptionStatus.ACTIVE);
        Subscription usdYearly = subscription("120.00", "USD", BillingPeriod.YEARLY, SubscriptionStatus.ACTIVE);
        Subscription eurMonthly = subscription("9.99", "EUR", BillingPeriod.MONTHLY, SubscriptionStatus.ACTIVE);
        Subscription cancelled = subscription("100.00", "USD", BillingPeriod.MONTHLY, SubscriptionStatus.CANCELLED);
        when(repository.findByStatus(SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(usdMonthly, usdYearly, eurMonthly));
        when(repository.findByStatusAndNextPaymentDateBetweenOrderByNextPaymentDateAsc(
                eq(SubscriptionStatus.ACTIVE), any(), any())).thenReturn(List.of());

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

    private Subscription subscription(String price, String currency, BillingPeriod period,
                                      SubscriptionStatus status) {
        return new Subscription(UUID.randomUUID(), "Service", null, new BigDecimal(price), currency,
                period, LocalDate.now().minusMonths(1), LocalDate.now().plusDays(5),
                SubscriptionCategory.OTHER, status);
    }
}

