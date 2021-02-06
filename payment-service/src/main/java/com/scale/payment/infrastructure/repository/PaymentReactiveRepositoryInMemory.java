package com.scale.payment.infrastructure.repository;

import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class PaymentReactiveRepositoryInMemory implements PaymentReactiveRepository {
    // TODO: Check these data types
    private final ConcurrentMap<ClientId, Card> clientSelectedCard = new ConcurrentHashMap<>();
    private final ConcurrentMap<String/*Card number*/, Card> cards = new ConcurrentHashMap<>();
    private final ConcurrentMap<String/*OrderId*/, Map<Double/*Amount*/, Card.Receipt>> receipts = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> addReceipt(Card.Receipt receipt) {
        if (!receipts.containsKey(receipt.getReference()))
            receipts.put(receipt.getReference(), new HashMap<>());
        receipts.get(receipt.getReference()).putIfAbsent(receipt.getAmount().getValue().doubleValue(), receipt);
        log.info("Receipt {} was stored in memory (reactive)", receipt.getNumber());
        return Mono.empty();
    }

    @Override
    public Mono<Card.Receipt> findReceipt(Order.OrderId id, Money amount) {
        if (!receipts.containsKey(id.value()))
            return Mono.empty();
        var entry = receipts.get(id.value());
        if (!entry.containsKey(amount.getValue().doubleValue()))
            return Mono.empty();
        return Mono.just(entry.get(amount.getValue().doubleValue()));
    }

    @Override
    public Mono<Void> addCard(Card card) {
        cards.put(card.getNumber(), card);
        return Mono.empty();
    }

    @Override
    public Mono<Card> findCard(String number, Short digit, Card.ExpirationDate expireAt) {
        var possibleCard = cards.get(number);
        if (possibleCard == null || !possibleCard.getDigit().equals(digit) || !possibleCard.getExpirationDate().equals(expireAt))
            return Mono.empty();

        return Mono.just(possibleCard);
    }

    @Override
    public Mono<Card> findCardByClient(ClientId clientId) {
        return Mono.justOrEmpty(clientSelectedCard.get(clientId));
    }

    @Override
    public Mono<Void> insertDefaultClientsWithCards() {
        cards.put("5ff67f73deff4f00079b7f84", new Card("5ff67f73deff4f00079b7f84", (short)333, new Card.ExpirationDate("10/2025"), Money.of(1_000_000_000.0)));
        cards.put("5ff873c1e77e950006a814af", new Card("5ff873c1e77e950006a814af", (short)444, new Card.ExpirationDate("01/2026"), Money.of(1_500_000_000.0)));
        cards.put("5ff873cae77e950006a814b1", new Card("5ff873cae77e950006a814b1", (short)555, new Card.ExpirationDate("08/2027"), Money.of(1_800_000_000.0)));

        clientSelectedCard.put(new ClientId("5ff867a5e77e950006a814ad"), cards.get("5ff67f73deff4f00079b7f84"));
        clientSelectedCard.put(new ClientId("5ff878f5e77e950006a814b3"), cards.get("5ff873c1e77e950006a814af"));
        clientSelectedCard.put(new ClientId("5ff87909e77e950006a814b5"), cards.get("5ff873cae77e950006a814b1"));

        return Mono.empty();
    }
}
