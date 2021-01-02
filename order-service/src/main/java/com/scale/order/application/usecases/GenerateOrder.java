package com.scale.order.application.usecases;

import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;
import com.scale.order.domain.repository.OrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class GenerateOrder {
    @NonNull OrderRepository orderRepository;

    public Order fromShoppingCart(ShoppingCart cart) {
        // TODO: Should receive a command containing dto with shopping cart data
        var order = cart.convert();
        orderRepository.store(order);
        return order;
    }

}
