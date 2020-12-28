package com.scale.check_out.domain.model;

public class CannotConvertShoppingCart extends CheckOutError {
    public CannotConvertShoppingCart() {
    }

    public CannotConvertShoppingCart(String message) {
        super(message);
    }
}
