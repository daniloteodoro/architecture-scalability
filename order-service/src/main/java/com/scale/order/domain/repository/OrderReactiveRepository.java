package com.scale.order.domain.repository;


import com.scale.domain.Order;
import reactor.core.publisher.Mono;

public interface OrderReactiveRepository {
    Mono<Order> store(Order order);
    Mono<Order> update(Order order);
    Mono<Order> load(Order.OrderId id);
}
