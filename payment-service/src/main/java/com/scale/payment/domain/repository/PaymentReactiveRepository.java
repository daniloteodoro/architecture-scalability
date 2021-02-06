package com.scale.payment.domain.repository;


import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import reactor.core.publisher.Mono;

public interface PaymentReactiveRepository {
    Mono<Void> addReceipt(Card.Receipt receipt);
    Mono<Void> addCard(Card card);
    Mono<Card.Receipt> findReceipt(Order.OrderId id, Money amount);
    Mono<Card> findCard(String number, Short digit, Card.ExpirationDate expireAt);
    Mono<Card> findCardByClient(ClientId clientId);

    Mono<Void> insertDefaultClientsWithCards();
}
