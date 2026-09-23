package com.example.subscriptiontracker.web;

import com.example.subscriptiontracker.security.AccountPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentAccountModelAdvice {

    @ModelAttribute("currentAccount")
    public AccountPrincipal currentAccount(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal
                ? principal
                : null;
    }
}
