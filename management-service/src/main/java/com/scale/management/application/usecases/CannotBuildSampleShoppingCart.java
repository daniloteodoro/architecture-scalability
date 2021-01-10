package com.scale.management.application.usecases;

import com.scale.management.application.ManagementError;

public class CannotBuildSampleShoppingCart extends ManagementError {
    public CannotBuildSampleShoppingCart() {
    }

    public CannotBuildSampleShoppingCart(String message) {
        super(message);
    }
}
