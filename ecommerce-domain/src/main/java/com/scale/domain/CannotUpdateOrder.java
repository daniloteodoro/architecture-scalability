package com.scale.domain;

public class CannotUpdateOrder extends DomainError {
    public CannotUpdateOrder() {
    }

    public CannotUpdateOrder(String message) {
        super(message);
    }
}
