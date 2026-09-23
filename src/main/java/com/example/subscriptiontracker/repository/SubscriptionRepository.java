package com.example.subscriptiontracker.repository;

import com.example.subscriptiontracker.domain.Subscription;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Subscription> findByIdAndUserId(UUID id, UUID userId);

    List<Subscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    List<Subscription> findByUserIdAndStatusAndNextPaymentDateBetweenOrderByNextPaymentDateAsc(
            UUID userId, SubscriptionStatus status, LocalDate from, LocalDate to);

    long countByUserId(UUID userId);
}
