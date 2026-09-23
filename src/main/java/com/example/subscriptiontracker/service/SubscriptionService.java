package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.api.dto.SubscriptionRequest;
import com.example.subscriptiontracker.domain.Subscription;
import com.example.subscriptiontracker.domain.SubscriptionStatus;
import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.exception.SubscriptionNotFoundException;
import com.example.subscriptiontracker.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository repository;
    private final CurrentUserService currentUserService;

    public SubscriptionService(SubscriptionRepository repository, CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Subscription create(SubscriptionRequest request) {
        User user = currentUserService.requireCurrentUser();
        String currency = normalizeCurrency(request.currency());
        Subscription subscription = new Subscription(
                UUID.randomUUID(),
                user,
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
        Subscription saved = repository.save(subscription);
        LOGGER.info("Subscription created: subscriptionId={}, userId={}", saved.getId(), user.getId());
        return saved;
    }

    public List<Subscription> findAll() {
        User user = currentUserService.requireCurrentUser();
        return repository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public Subscription findById(UUID id) {
        User user = currentUserService.requireCurrentUser();
        return repository.findByIdAndUserId(id, user.getId()).orElseThrow(() -> {
            LOGGER.warn("Subscription access rejected: subscriptionId={}, userId={}", id, user.getId());
            return new SubscriptionNotFoundException(id);
        });
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
        LOGGER.info("Subscription updated: subscriptionId={}, userId={}", id, subscription.getUser().getId());
        return subscription;
    }

    @Transactional
    public Subscription cancel(UUID id) {
        Subscription subscription = findById(id);
        subscription.cancel();
        LOGGER.info("Subscription cancelled: subscriptionId={}, userId={}", id, subscription.getUser().getId());
        return subscription;
    }

    @Transactional
    public void delete(UUID id) {
        Subscription subscription = findById(id);
        repository.delete(subscription);
        LOGGER.info("Subscription deleted: subscriptionId={}, userId={}", id, subscription.getUser().getId());
    }

    public List<Subscription> findUpcomingPayments(int days) {
        User user = currentUserService.requireCurrentUser();
        LocalDate today = LocalDate.now();
        return repository.findByUserIdAndStatusAndNextPaymentDateBetweenOrderByNextPaymentDateAsc(
                user.getId(), SubscriptionStatus.ACTIVE, today, today.plusDays(days));
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
