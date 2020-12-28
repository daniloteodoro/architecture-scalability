package com.scale.check_out.domain.model;

public class CheckOutError extends RuntimeException {
    public CheckOutError() {
    }

    public CheckOutError(String message) {
        super(message);
    }
}
