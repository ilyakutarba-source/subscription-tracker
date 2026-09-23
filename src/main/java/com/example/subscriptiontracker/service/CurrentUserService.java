package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.domain.User;
import com.example.subscriptiontracker.exception.CurrentUserNotFoundException;
import com.example.subscriptiontracker.repository.UserRepository;
import com.example.subscriptiontracker.security.AccountPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CurrentUserService {

    private final UserRepository repository;

    public CurrentUserService(UserRepository repository) {
        this.repository = repository;
    }

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new CurrentUserNotFoundException();
        }
        return repository.findById(principal.getUserId())
                .filter(User::isEnabled)
                .orElseThrow(CurrentUserNotFoundException::new);
    }
}
