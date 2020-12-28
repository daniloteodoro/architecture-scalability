package com.scale.domain;

public class CannotConvertShoppingCart extends DomainError {
    public CannotConvertShoppingCart() {
    }

    public CannotConvertShoppingCart(String message) {
        super(message);
    }
}
