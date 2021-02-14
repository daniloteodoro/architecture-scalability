package com.scale.order.infrastructure.repository;

import com.scale.domain.Order;
import com.scale.order.application.usecases.OrderNotFound;
import com.scale.order.domain.repository.OrderReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class OrderReactiveRepositoryInMemory implements OrderReactiveRepository {
    private final ConcurrentMap<Order.OrderId, Order> orders = new ConcurrentHashMap<>();

    @Override
    public Mono<Order> store(Order order) {
        orders.put(order.getId(), order);
        log.info("Order {} was stored in memory", order.getId());
        return Mono.just(order);
    }

    @Override
    public Mono<Order> update(Order order) {
        if (!orders.containsKey(order.getId()))
            throw new OrderNotFound(order.getId());
        orders.replace(order.getId(), order);
        log.info("Order {} was updated in memory", order.getId());
        return Mono.just(order);
    }

    @Override
    public Mono<Order> load(Order.OrderId id) {
        var order = orders.get(id);
        if (order == null)
            return Mono.empty();

        return Mono.just(order);
    }
}
