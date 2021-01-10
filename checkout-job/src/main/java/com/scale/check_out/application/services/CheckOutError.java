package com.scale.check_out.application.services;

public class CheckOutError extends RuntimeException {
    public CheckOutError() {
    }

    public CheckOutError(String message) {
        super(message);
    }
}
