package com.scale.domain;

public class InvalidSession extends DomainError {
    public InvalidSession() {
    }

    public InvalidSession(String message) {
        super(message);
    }
}
