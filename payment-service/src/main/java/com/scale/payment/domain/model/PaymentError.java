package com.scale.payment.domain.model;

public class PaymentError extends RuntimeException {
    public PaymentError() {
    }

    public PaymentError(String message) {
        super(message);
    }
}
