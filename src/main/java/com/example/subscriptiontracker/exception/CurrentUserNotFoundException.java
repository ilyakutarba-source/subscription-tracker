package com.example.subscriptiontracker.exception;

public class CurrentUserNotFoundException extends RuntimeException {

    public CurrentUserNotFoundException() {
        super("The authenticated account is no longer available.");
    }
}
