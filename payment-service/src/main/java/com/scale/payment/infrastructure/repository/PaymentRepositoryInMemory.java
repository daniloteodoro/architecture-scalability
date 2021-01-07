package com.scale.payment.infrastructure.repository;

import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class PaymentRepositoryInMemory implements PaymentRepository {
    private final Map<String, Card> cards = new HashMap<>();
    private final Map<String, Map<Double, Card.Receipt>> receipts = new HashMap<>();

    @Override
    public void addReceipt(Card.Receipt receipt) {
        if (!receipts.containsKey(receipt.getReference()))
            receipts.put(receipt.getReference(), new HashMap<>());
        receipts.get(receipt.getReference()).putIfAbsent(receipt.getAmount().getValue().doubleValue(), receipt);
        log.info("Receipt {} was stored in memory", receipt.getNumber());
    }

    @Override
    public Optional<Card.Receipt> findReceipt(Order.OrderId id, Money amount) {
        if (!receipts.containsKey(id.value()))
            return Optional.empty();
        var entry = receipts.get(id.value());
        if (!entry.containsKey(amount.getValue().doubleValue()))
            return Optional.empty();
        return Optional.of(entry.get(amount.getValue().doubleValue()));
    }

    @Override
    public void addCard(Card card) {
        cards.put(card.getNumber(), card);
    }

    @Override
    public Optional<Card> findCard(String number, Short digit, Card.ExpirationDate expireAt) {
        var possibleCard = cards.get(number);
        if (possibleCard == null || !possibleCard.getDigit().equals(digit) || !possibleCard.getExpirationDate().equals(expireAt))
            return Optional.empty();

        return Optional.of(possibleCard);
    }

}
