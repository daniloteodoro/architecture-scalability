package com.scale.management.domain.model;

public class ManagementError extends RuntimeException {
    public ManagementError() {
    }

    public ManagementError(String message) {
        super(message);
    }
}
