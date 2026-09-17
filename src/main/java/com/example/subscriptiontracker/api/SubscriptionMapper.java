package com.example.subscriptiontracker.api;

import com.example.subscriptiontracker.api.dto.SubscriptionResponse;
import com.example.subscriptiontracker.domain.Subscription;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getName(),
                subscription.getDescription(),
                subscription.getPrice(),
                subscription.getCurrency(),
                subscription.getBillingPeriod(),
                subscription.getStartDate(),
                subscription.getNextPaymentDate(),
                subscription.getCategory(),
                subscription.getStatus(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }
}

