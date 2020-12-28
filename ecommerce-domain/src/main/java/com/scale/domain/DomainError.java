package com.scale.domain;

public class DomainError extends RuntimeException {
    public DomainError() {
    }

    public DomainError(String message) {
        super(message);
    }
}
