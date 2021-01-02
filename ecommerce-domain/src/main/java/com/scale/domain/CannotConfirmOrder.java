package com.scale.domain;

public class CannotConfirmOrder extends DomainError {
    public CannotConfirmOrder() {
    }

    public CannotConfirmOrder(String message) {
        super(message);
    }
}
