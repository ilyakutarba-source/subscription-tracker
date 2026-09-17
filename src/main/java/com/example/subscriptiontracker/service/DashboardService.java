package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.api.SubscriptionMapper;
import com.example.subscriptiontracker.api.dto.DashboardResponse;
import com.example.subscriptiontracker.domain.BillingPeriod;
import com.example.subscriptiontracker.domain.Subscription;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import com.example.subscriptiontracker.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private final SubscriptionRepository repository;
    private final SubscriptionMapper mapper;

    public DashboardService(SubscriptionRepository repository, SubscriptionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public DashboardResponse getDashboard(int days) {
        List<Subscription> active = repository.findByStatus(SubscriptionStatus.ACTIVE);
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        Map<String, BigDecimal> yearly = new LinkedHashMap<>();

        active.forEach(subscription -> {
            monthly.merge(subscription.getCurrency(), monthlyEquivalent(subscription), BigDecimal::add);
            yearly.merge(subscription.getCurrency(), yearlyEquivalent(subscription), BigDecimal::add);
        });
        monthly.replaceAll((currency, amount) -> amount.setScale(2, RoundingMode.HALF_UP));
        yearly.replaceAll((currency, amount) -> amount.setScale(2, RoundingMode.HALF_UP));

        LocalDate today = LocalDate.now();
        var upcoming = repository.findByStatusAndNextPaymentDateBetweenOrderByNextPaymentDateAsc(
                        SubscriptionStatus.ACTIVE, today, today.plusDays(days))
                .stream()
                .map(mapper::toResponse)
                .toList();

        return new DashboardResponse(active.size(), monthly, yearly, upcoming);
    }

    BigDecimal monthlyEquivalent(Subscription subscription) {
        return subscription.getBillingPeriod() == BillingPeriod.YEARLY
                ? subscription.getPrice().divide(MONTHS_PER_YEAR, 8, RoundingMode.HALF_UP)
                : subscription.getPrice();
    }

    BigDecimal yearlyEquivalent(Subscription subscription) {
        return subscription.getBillingPeriod() == BillingPeriod.MONTHLY
                ? subscription.getPrice().multiply(MONTHS_PER_YEAR)
                : subscription.getPrice();
    }
}

