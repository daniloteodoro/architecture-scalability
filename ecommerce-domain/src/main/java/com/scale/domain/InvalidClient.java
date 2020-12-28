package com.scale.domain;

public class InvalidClient extends DomainError {
    public InvalidClient() {
    }

    public InvalidClient(String message) {
        super(message);
    }
}
