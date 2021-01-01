package com.scale.order.infrastructure.repository;

import com.scale.domain.Order;
import com.scale.order.application.exceptions.OrderError;
import com.scale.order.application.usecase.OrderNotFound;
import com.scale.order.domain.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class OrderRepositoryInMemory implements OrderRepository {
    private final Map<Order.OrderId, Order> orders = new HashMap<>();

    @Override
    public void store(Order order) {
        orders.put(order.getId(), order);
        log.info("Order {} was stored in memory", order.getId());
    }

    @Override
    public void update(Order order) {
        if (!orders.containsKey(order.getId()))
            throw new OrderNotFound(order.getId());
        orders.replace(order.getId(), order);
        log.info("Order {} was updated in memory", order.getId());
    }

    @Override
    public Optional<Order> load(Order.OrderId id) {
        return Optional.ofNullable(orders.get(id));
    }
}
