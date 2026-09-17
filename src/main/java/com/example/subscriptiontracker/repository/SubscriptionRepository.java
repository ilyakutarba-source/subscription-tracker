package com.example.subscriptiontracker.repository;

import com.example.subscriptiontracker.domain.Subscription;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findAllByOrderByCreatedAtDesc();

    List<Subscription> findByStatus(SubscriptionStatus status);

    List<Subscription> findByStatusAndNextPaymentDateBetweenOrderByNextPaymentDateAsc(
            SubscriptionStatus status, LocalDate from, LocalDate to);
}

