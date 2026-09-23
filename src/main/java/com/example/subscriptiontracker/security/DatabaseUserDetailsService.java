package com.example.subscriptiontracker.security;

import com.example.subscriptiontracker.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository repository;

    public DatabaseUserDetailsService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return repository.findByEmail(normalizedEmail)
                .map(AccountPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
    }
}
