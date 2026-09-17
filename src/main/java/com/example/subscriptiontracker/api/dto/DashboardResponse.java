package com.example.subscriptiontracker.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long activeSubscriptions,
        Map<String, BigDecimal> monthlyCost,
        Map<String, BigDecimal> yearlyCost,
        List<SubscriptionResponse> upcomingPayments
) {
}

