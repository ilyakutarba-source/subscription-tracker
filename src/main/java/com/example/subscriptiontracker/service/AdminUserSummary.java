package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public record AdminUserSummary(
        UUID id,
        String email,
        String displayName,
        UserRole role,
        boolean enabled,
        long subscriptionCount,
        Instant createdAt,
        boolean currentAccount
) {
}
