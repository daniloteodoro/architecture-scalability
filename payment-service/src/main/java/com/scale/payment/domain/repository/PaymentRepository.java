package com.scale.payment.domain.repository;


import com.scale.domain.Order;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.model.Receipt;

import java.util.Optional;

public interface PaymentRepository {
    void add(String request);
    Optional<Receipt> getReceipt(Order.OrderId id, Money amount);
}
