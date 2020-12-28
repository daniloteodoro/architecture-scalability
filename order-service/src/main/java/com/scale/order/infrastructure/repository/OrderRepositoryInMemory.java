package com.scale.order.infrastructure.repository;

import com.scale.domain.Order;
import com.scale.order.domain.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class OrderRepositoryInMemory implements OrderRepository {

    @Override
    public void store(Order order) {
        log.info("Order {} was stored in memory", order.getId());
    }

    @Override
    public Optional<Order> load(Order.OrderId id) {
        log.warn("Order {} was not found", id);
        return Optional.empty();
    }
}
