package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.domain.UserRole;
import com.example.subscriptiontracker.repository.SubscriptionRepository;
import com.example.subscriptiontracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CurrentUserService currentUserService;

    public AdminService(UserRepository userRepository, SubscriptionRepository subscriptionRepository,
                        CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.currentUserService = currentUserService;
    }

    public AdminOverview getOverview() {
        return new AdminOverview(
                userRepository.count(),
                userRepository.countByEnabledTrue(),
                userRepository.countByRole(UserRole.ADMIN),
                subscriptionRepository.count()
        );
    }

    public List<AdminUserSummary> findUsers() {
        UUID currentUserId = currentUserService.requireCurrentUser().getId();
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(user -> new AdminUserSummary(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole(),
                        user.isEnabled(),
                        subscriptionRepository.countByUserId(user.getId()),
                        user.getCreatedAt(),
                        user.getId().equals(currentUserId)
                ))
                .toList();
    }

    @Transactional
    public void changeRole(UUID userId, UserRole role) {
        User currentUser = currentUserService.requireCurrentUser();
        User target = requireUser(userId);
        ensureNotCurrentAccount(currentUser, target, "You cannot change your own role.");
        target.changeRole(role);
        LOGGER.info("Admin changed user role: adminUserId={}, targetUserId={}, role={}",
                currentUser.getId(), target.getId(), role);
    }

    @Transactional
    public void changeEnabled(UUID userId, boolean enabled) {
        User currentUser = currentUserService.requireCurrentUser();
        User target = requireUser(userId);
        ensureNotCurrentAccount(currentUser, target, "You cannot block your own account.");
        target.changeEnabled(enabled);
        LOGGER.info("Admin changed user status: adminUserId={}, targetUserId={}, enabled={}",
                currentUser.getId(), target.getId(), enabled);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private void ensureNotCurrentAccount(User currentUser, User target, String message) {
        if (currentUser.getId().equals(target.getId())) {
            throw new IllegalStateException(message);
        }
    }
}
