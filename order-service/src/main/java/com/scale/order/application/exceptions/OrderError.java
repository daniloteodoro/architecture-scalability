package com.scale.order.application.exceptions;

public class OrderError extends RuntimeException {
    public OrderError() {
    }

    public OrderError(String message) {
        super(message);
    }
}
