package com.scale.payment.domain.repository;


import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.Money;

import java.util.Optional;

public interface PaymentRepository {
    void add(Card.Receipt receipt);
    Optional<Card.Receipt> findReceipt(Order.OrderId id, Money amount);
    Optional<Card> findCard(String number, Short digit, Card.ExpirationDate expireAt);
}
