package com.scale.order.application.usecases;

import com.scale.domain.Order;
import com.scale.order.domain.repository.OrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class UpdateOrder {
    @NonNull OrderRepository orderRepository;

    public void changeAddress(Order.OrderId id, String newAddress) {
        var order = orderRepository.load(id)
                .orElseThrow(() -> new OrderNotFound(id));
        // TODO: set address using domain object, log possible address changes
        order.setFullAddress(newAddress);
        orderRepository.update(order);
    }

}
