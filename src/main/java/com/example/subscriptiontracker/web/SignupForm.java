package com.example.subscriptiontracker.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupForm {

    @NotBlank(message = "Enter your display name.")
    @Size(max = 120, message = "Display name must be 120 characters or fewer.")
    private String displayName;

    @NotBlank(message = "Enter your email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 320, message = "Email must be 320 characters or fewer.")
    private String email;

    @NotBlank(message = "Enter a password.")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
    private String password;

    @NotBlank(message = "Confirm your password.")
    private String confirmPassword;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
