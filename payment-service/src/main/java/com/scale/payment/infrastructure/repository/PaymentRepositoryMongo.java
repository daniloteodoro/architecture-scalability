package com.scale.payment.infrastructure.repository;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.*;
import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.time.ZonedDateTime;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

@Slf4j
public class PaymentRepositoryMongo implements PaymentRepository {
    private final @NonNull MongoClient client;
    private final @NonNull MongoCollection<Document> cards;
    private final @NonNull MongoCollection<Document> receipts;

    public PaymentRepositoryMongo(@NonNull MongoClient client, @NonNull MongoDatabase db) {
        this.client = client;
        this.cards = db.getCollection("cards");
        this.receipts = db.getCollection("receipts");
    }

    private Bson findReceiptInMongo(Order.OrderId id, Money amount) {
        return and(eq("reference", id.value()),
                eq("amount", amount.getValue().doubleValue()));
    }

    private Bson findCardInMongo(String number, Short digit, Card.ExpirationDate expirationDate) {
        return and(eq("_id", new ObjectId(number)),
                eq("digit", digit),
                eq("expiration_date", expirationDate.getValue()));
    }

    @Override
    public Optional<Card.Receipt> findReceipt(Order.OrderId id, Money amount) {
        var paymentDoc = receipts.find(findReceiptInMongo(id, amount))
                .first();
        if (paymentDoc == null)
            return Optional.empty();

        return Optional.of(deserializePaymentReceipt(paymentDoc));
    }

    @Override
    public void addReceipt(Card.Receipt receipt) {

        final ClientSession newSession = client.startSession();
        TransactionOptions txnOptions = TransactionOptions.builder()
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.SNAPSHOT)
                .writeConcern(WriteConcern.MAJORITY)
                .build();

        TransactionBody<String> changes = () -> {
            receipts.insertOne(serializePaymentReceipt(receipt));
            cards.updateOne(eq("_id", new ObjectId(receipt.getCard().getNumber())),
                    set("limit", receipt.getCard().getLimit().getValue().doubleValue()));
            return "done";
        };

        newSession.withTransaction(changes, txnOptions);

        log.info("Receipt {} with reference to {} was stored in Mongo", receipt.getNumber(), receipt.getReference());
    }

    @Override
    public void addCard(Card card) {
        cards.insertOne(serializeCard(card));
        log.info("Card {} was stored in Mongo", card.getNumber());
    }

    private Document serializeCard(Card card) {
        return new Document("_id", new ObjectId(card.getNumber()))
                .append("digit", card.getDigit())
                .append("expiration_date", card.getExpirationDate().getValue())
                .append("limit", card.getLimit().getValue().doubleValue());
    }

    @Override
    public Optional<Card> findCard(String number, Short digit, Card.ExpirationDate expireAt) {
        var cardDoc = cards.find(findCardInMongo(number, digit, expireAt))
                .first();
        if (cardDoc == null)
            return Optional.empty();

        return Optional.of(deserializeCard(cardDoc));
    }

    private Document serializePaymentReceipt(Card.Receipt receipt) {
        return new Document("_id", receipt.getNumber())
                .append("card_id", new ObjectId(receipt.getCard().getNumber()))
                .append("time", receipt.getTime().toString())
                .append("reference", receipt.getReference())
                .append("amount", receipt.getAmount().getValue().doubleValue());
    }

    private Card deserializeCard(Document doc) {
        return new Card(doc.getObjectId("_id").toString(),
                doc.getInteger("digit").shortValue(),
                new Card.ExpirationDate(doc.getString("expiration_date")),
                Money.of(Double.parseDouble(doc.get("limit").toString())));  // getDouble has issues when value is an integer...
    }

    private Optional<Card> findCard(ObjectId cardId) {
        var possibleCard = cards.find(eq("_id", cardId))
                .first();
        if (possibleCard == null)
            return Optional.empty();

        return Optional.of(deserializeCard(possibleCard));
    }

    private Card.Receipt deserializePaymentReceipt(Document doc) {
        Card associatedCard = findCard((ObjectId) doc.get("card_id"))
                .orElseThrow(() -> new Card.CardError("Card was not found: " + doc.getString("card_id")));

        return associatedCard.regenerateReceipt(doc.getString("_id"),
                ZonedDateTime.parse(doc.getString("time")),
                Money.of(doc.getDouble("amount")),
                doc.getString("reference"));
    }
}
