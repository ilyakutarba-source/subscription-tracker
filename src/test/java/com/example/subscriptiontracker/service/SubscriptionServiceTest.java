package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.api.dto.SubscriptionRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository repository;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(repository);
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
        assertThat(created.getName()).isEqualTo("ChatGPT");
        assertThat(created.getDescription()).isEqualTo("AI assistant");
        assertThat(created.getCurrency()).isEqualTo("USD");
        assertThat(created.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
}

