package com.example.subscriptiontracker.service;

public record AdminOverview(
        long totalUsers,
        long enabledUsers,
        long adminUsers,
        long totalSubscriptions
) {
}
