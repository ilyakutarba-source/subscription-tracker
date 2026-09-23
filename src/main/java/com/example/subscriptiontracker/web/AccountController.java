package com.example.subscriptiontracker.web;

import com.example.subscriptiontracker.exception.DuplicateEmailException;
import com.example.subscriptiontracker.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AccountController {

    private final RegistrationService registrationService;

    public AccountController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        return isSignedIn(authentication) ? "redirect:/" : "account/login";
    }

    @GetMapping("/signup")
    public String signup(Model model, Authentication authentication) {
        if (isSignedIn(authentication)) {
            return "redirect:/";
        }
        model.addAttribute("signupForm", new SignupForm());
        return "account/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid SignupForm signupForm, BindingResult bindingResult) {
        if (!signupForm.getPassword().equals(signupForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (bindingResult.hasErrors()) {
            return "account/signup";
        }
        try {
            registrationService.register(signupForm.getEmail(), signupForm.getPassword(),
                    signupForm.getDisplayName());
        } catch (DuplicateEmailException exception) {
            bindingResult.rejectValue("email", "email.duplicate", exception.getMessage());
            return "account/signup";
        }
        return "redirect:/login?registered";
    }

    private boolean isSignedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
