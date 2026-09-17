package com.example.subscriptiontracker.api.dto;

import com.example.subscriptiontracker.domain.BillingPeriod;
import com.example.subscriptiontracker.domain.SubscriptionCategory;
import com.example.subscriptiontracker.domain.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        BillingPeriod billingPeriod,
        LocalDate startDate,
        LocalDate nextPaymentDate,
        SubscriptionCategory category,
        SubscriptionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}

