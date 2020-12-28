package com.scale.order.domain.repository;


import com.scale.domain.Order;

import java.util.Optional;

public interface OrderRepository {
    void store(Order order);
    Optional<Order> load(Order.OrderId id);
}
