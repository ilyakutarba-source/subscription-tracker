package com.example.subscriptiontracker.security;

import com.example.subscriptiontracker.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class AccountPrincipal implements UserDetails, CredentialsContainer {

    private final UUID userId;
    private final String email;
    private String passwordHash;
    private final String displayName;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    private AccountPrincipal(UUID userId, String email, String passwordHash, String displayName,
                             boolean enabled, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public static AccountPrincipal from(User user) {
        return new AccountPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getDisplayName(),
                user.isEnabled(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    public UUID getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public boolean isAdmin() {
        return authorities.stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    @Override
    public String toString() {
        return "AccountPrincipal{userId=" + userId + '}';
    }
}
