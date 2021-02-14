package com.scale.order.application.usecases;

import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;
import com.scale.order.domain.repository.OrderReactiveRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class GenerateOrderReactive {
    @NonNull OrderReactiveRepository orderRepository;

    public Mono<Order> fromShoppingCart(ShoppingCart cart) {
        try {
            var order = cart.convert();
            return orderRepository.store(order);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

}
