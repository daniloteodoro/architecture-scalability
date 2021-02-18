package com.scale.order.domain.model;

import com.scale.domain.Order;
import com.scale.order.application.exceptions.OrderError;
import lombok.NonNull;
import lombok.Value;

@Value
public class PaidOrder {
    @NonNull Order.OrderId orderId;
    @NonNull String receiptNumber;

    public static final PaidOrder INVALID = new PaidOrder(Order.OrderId.of(""), "");

    private PaidOrder(Order.OrderId orderId, String receiptNumber) {
        this.orderId = orderId;
        this.receiptNumber = receiptNumber;
    }

    public static PaidOrder of(Order.OrderId orderId, String receiptNumber) {
        if (orderId == null || orderId.value().isBlank())
            throw new OrderError("Order is must not be blank");
        if (receiptNumber == null || receiptNumber.isBlank())
            throw new OrderError("Receipt number must not be blank");
        return new PaidOrder(orderId, receiptNumber);
    }

    public boolean isValid() {
        return (orderId != null) &&
                (!orderId.value().isBlank()) &&
                (!receiptNumber.isBlank());
    }
}
