package com.scale.domain;

public class CannotCreateProduct extends DomainError {
    public CannotCreateProduct() {
    }

    public CannotCreateProduct(String message) {
        super(message);
    }
}
