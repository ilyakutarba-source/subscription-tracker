package com.example.subscriptiontracker.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("An account with this email already exists.");
    }
}
