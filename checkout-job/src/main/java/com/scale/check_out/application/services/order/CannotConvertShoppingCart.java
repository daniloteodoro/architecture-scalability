package com.scale.check_out.application.services.order;

import com.scale.check_out.application.services.CheckOutError;

public class CannotConvertShoppingCart extends CheckOutError {
    public CannotConvertShoppingCart() {
    }

    public CannotConvertShoppingCart(String message) {
        super(message);
    }
}
