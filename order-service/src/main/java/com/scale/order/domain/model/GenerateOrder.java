package com.scale.order.domain.model;

import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;
import com.scale.order.domain.repository.OrderRepository;

import java.util.Objects;

public class GenerateOrder {

    private final OrderRepository orderRepository;

    public GenerateOrder(OrderRepository orderRepository) {
        super();
        this.orderRepository = Objects.requireNonNull(orderRepository, "Order repository is mandatory");
    }

    public Order fromShoppingCart(ShoppingCart cart) {
        // TODO: Should receive a command containing dto with shopping cart data
        var order = cart.convert();
        orderRepository.store(order);
        return order;
    }

}
