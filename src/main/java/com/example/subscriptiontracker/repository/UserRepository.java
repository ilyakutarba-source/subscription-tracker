package com.example.subscriptiontracker.repository;

import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByCreatedAtDesc();

    long countByEnabledTrue();

    long countByRole(UserRole role);
}
