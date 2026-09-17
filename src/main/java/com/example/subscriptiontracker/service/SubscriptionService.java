package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.api.dto.SubscriptionRequest;
import com.example.subscriptiontracker.domain.Subscription;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import com.example.subscriptiontracker.exception.SubscriptionNotFoundException;
import com.example.subscriptiontracker.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository repository;

    public SubscriptionService(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Subscription create(SubscriptionRequest request) {
        String currency = normalizeCurrency(request.currency());
        Subscription subscription = new Subscription(
                UUID.randomUUID(),
                request.name().trim(),
                normalizeDescription(request.description()),
                request.price(),
                currency,
                request.billingPeriod(),
                request.startDate(),
                request.nextPaymentDate(),
                request.category(),
                request.status() == null ? SubscriptionStatus.ACTIVE : request.status()
        );
        return repository.save(subscription);
    }

    public List<Subscription> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public Subscription findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new SubscriptionNotFoundException(id));
    }

    @Transactional
    public Subscription update(UUID id, SubscriptionRequest request) {
        Subscription subscription = findById(id);
        subscription.update(
                request.name().trim(),
                normalizeDescription(request.description()),
                request.price(),
                normalizeCurrency(request.currency()),
                request.billingPeriod(),
                request.startDate(),
                request.nextPaymentDate(),
                request.category(),
                request.status() == null ? subscription.getStatus() : request.status()
        );
        return subscription;
    }

    @Transactional
    public Subscription cancel(UUID id) {
        Subscription subscription = findById(id);
        subscription.cancel();
        return subscription;
    }

    @Transactional
    public void delete(UUID id) {
        Subscription subscription = findById(id);
        repository.delete(subscription);
    }

    public List<Subscription> findUpcomingPayments(int days) {
        LocalDate today = LocalDate.now();
        return repository.findByStatusAndNextPaymentDateBetweenOrderByNextPaymentDateAsc(
                SubscriptionStatus.ACTIVE, today, today.plusDays(days));
    }

    private String normalizeCurrency(String value) {
        String currency = value.toUpperCase(Locale.ROOT);
        Currency.getInstance(currency);
        return currency;
    }

    private String normalizeDescription(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

