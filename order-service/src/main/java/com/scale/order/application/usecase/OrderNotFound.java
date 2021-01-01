package com.scale.order.application.usecase;

import com.scale.domain.Order;

public class OrderNotFound extends RuntimeException {
    public OrderNotFound(Order.OrderId id) {
        super(String.format("Order %s was not found", id));
    }

    public OrderNotFound(String message) {
        super(message);
    }
}
