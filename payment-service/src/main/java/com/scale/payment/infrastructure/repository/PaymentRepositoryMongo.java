package com.scale.payment.infrastructure.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.ZonedDateTime;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class PaymentRepositoryMongo implements PaymentRepository {
    private final @NonNull MongoCollection<Document> cards;
    private final @NonNull MongoCollection<Document> receipts;

    public PaymentRepositoryMongo(@NonNull MongoDatabase db) {
        this.cards = db.getCollection("cards");
        this.receipts = db.getCollection("receipts");
    }

    private Bson findByReferenceAndAmount(Order.OrderId id, Money amount) {
        return and(eq("reference", id.value()),
                    eq("amount", amount.getValue().doubleValue()));
    }

    @Override
    public Optional<Card.Receipt> getReceipt(Order.OrderId id, Money amount) {
        var paymentDoc = receipts.find(findByReferenceAndAmount(id, amount))
                .first();
        if (paymentDoc == null)
            return Optional.empty();

        return Optional.of(deserializePaymentReceipt(paymentDoc));
    }

    @Override
    public void add(Card.Receipt receipt) {
        receipts.insertOne(serializePaymentReceipt(receipt));
        log.info("Receipt {} with reference to {} was stored in Mongo", receipt.getNumber(), receipt.getReference());
    }

    private Document serializePaymentReceipt(Card.Receipt receipt) {
        return new Document("_id", receipt.getNumber())
                .append("card_id", receipt.getCard().getNumber())
                .append("time", receipt.getTime().toString())
                .append("reference", receipt.getReference())
                .append("amount", receipt.getAmount().getValue().doubleValue());
    }

    private Card deserializeCard(Document doc) {
        return new Card(doc.getString("card_id"),
                doc.getInteger("digit").shortValue(),
                new Card.ExpirationDate(doc.getString("expire_at")),
                Money.of(doc.getDouble("limit")));
    }

    private Optional<Card> findCard(String cardId) {
        var possibleCard = cards.find(eq("card_id", cardId))
                .first();
        if (possibleCard == null)
            return Optional.empty();

        return Optional.of(deserializeCard(possibleCard));
    }

    private Card.Receipt deserializePaymentReceipt(Document doc) {
        Card associatedCard = findCard(doc.getString("card_id"))
                .orElseThrow(() -> new Card.CardError("Card was not found: " + doc.getString("card_id")));

        return associatedCard.regenerateReceipt(doc.getString("number"),
                ZonedDateTime.parse(doc.getString("time")),
                Money.of(doc.getDouble("amount")),
                doc.getString("reference"));
    }
}
