package com.scale.order.application.usecases;

import com.scale.domain.Order;
import com.scale.order.domain.repository.OrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ConfirmOrder {
    @NonNull OrderRepository orderRepository;

    public void withPaymentReceipt(Order.OrderId id, String receipt) {
        var order = orderRepository.load(id)
                .orElseThrow(() -> new OrderNotFound(id));
        order.confirm(receipt);
        orderRepository.update(order);
    }

}
