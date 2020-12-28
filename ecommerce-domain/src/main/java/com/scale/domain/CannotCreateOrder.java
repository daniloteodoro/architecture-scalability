package com.scale.domain;

public class CannotCreateOrder extends DomainError {
    public CannotCreateOrder() {
    }

    public CannotCreateOrder(String message) {
        super(message);
    }
}
