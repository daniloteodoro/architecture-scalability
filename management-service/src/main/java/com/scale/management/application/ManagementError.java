package com.scale.management.application;

public class ManagementError extends RuntimeException {
    public ManagementError() {
    }

    public ManagementError(String message) {
        super(message);
    }
}
