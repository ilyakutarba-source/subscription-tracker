package com.example.subscriptiontracker.api.dto;

import com.example.subscriptiontracker.domain.BillingPeriod;
import com.example.subscriptiontracker.domain.SubscriptionCategory;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotBlank @Pattern(regexp = "(?i)[A-Z]{3}", message = "must be a 3-letter ISO currency code") String currency,
        @NotNull BillingPeriod billingPeriod,
        @NotNull LocalDate startDate,
        @NotNull LocalDate nextPaymentDate,
        @NotNull SubscriptionCategory category,
        SubscriptionStatus status
) {
}

