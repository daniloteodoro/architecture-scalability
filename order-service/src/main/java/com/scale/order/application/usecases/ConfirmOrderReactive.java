package com.scale.order.application.usecases;

import com.scale.domain.Order;
import com.scale.order.domain.model.PaidOrder;
import com.scale.order.domain.repository.OrderReactiveRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class ConfirmOrderReactive {
    @NonNull OrderReactiveRepository orderRepository;

    public Mono<Order> withPaymentReceipt(PaidOrder paidOrder) {
        return orderRepository.load(paidOrder.getOrderId())
                .flatMap(order -> {
                    try {
                        order.confirm(paidOrder.getReceiptNumber());
                        return orderRepository.update(order);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .switchIfEmpty(Mono.error(new OrderNotFound(paidOrder.getOrderId())));
    }

}
