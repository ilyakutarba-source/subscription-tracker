package com.example.subscriptiontracker.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEventLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationEventLogger.class);

    @EventListener
    public void authenticationSucceeded(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof AccountPrincipal principal) {
            LOGGER.info("Authentication succeeded: userId={}", principal.getUserId());
        }
    }

    @EventListener
    public void authenticationFailed(AbstractAuthenticationFailureEvent event) {
        LOGGER.warn("Authentication failed");
    }

    @EventListener
    public void logoutSucceeded(LogoutSuccessEvent event) {
        if (event.getAuthentication() != null
                && event.getAuthentication().getPrincipal() instanceof AccountPrincipal principal) {
            LOGGER.info("Logout succeeded: userId={}", principal.getUserId());
        }
    }
}
