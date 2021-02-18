package com.scale.payment.infrastructure.repository;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
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
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@Slf4j
public class PaymentRepositoryMongo implements PaymentRepository {
    private final UpdateOptions optionsWithUpsert = new UpdateOptions().upsert(true);

    private final @NonNull MongoDatabase db;
    private final @NonNull MongoClient mongoClient;
    private final @NonNull MongoCollection<Document> clients;
    private final @NonNull MongoCollection<Document> cards;
    private final @NonNull MongoCollection<Document> receipts;

    public PaymentRepositoryMongo(@NonNull MongoClient mongoClient, @NonNull MongoDatabase db) {
        this.mongoClient = mongoClient;
        this.db = db;
        this.cards = db.getCollection("cards");
        this.receipts = db.getCollection("receipts");
        this.clients = db.getCollection("clients");
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

//        final ClientSession newSession = mongoClient.startSession();
//        TransactionOptions txnOptions = TransactionOptions.builder()
//                .readPreference(ReadPreference.primary())
//                .readConcern(ReadConcern.SNAPSHOT)
//                .writeConcern(WriteConcern.MAJORITY)
//                .build();
//
//        TransactionBody<String> changes = () -> {
//            receipts.insertOne(serializePaymentReceipt(receipt));
//            cards.updateOne(eq("_id", new ObjectId(receipt.getCard().getNumber())),
//                    set("limit", receipt.getCard().getLimit().getValue().doubleValue()));
//            return "done";
//        };

        // Commented to stay in sync with the reactive version - which lacks this transaction support https://jira.mongodb.org/browse/JAVA-3539
//        newSession.withTransaction(changes, txnOptions);

        receipts.insertOne(serializePaymentReceipt(receipt));
        cards.updateOne(eq("_id", new ObjectId(receipt.getCard().getNumber())),
                set("limit", receipt.getCard().getLimit().getValue().doubleValue()));

        log.info("Receipt {} with reference to {} was stored in Mongo", receipt.getNumber(), receipt.getReference());
    }

    @Override
    public void addCard(Card card) {
        cards.insertOne(serializeCard(card));
        log.info("Card {} was stored in Mongo", card.getNumber());
    }

    @Override
    public Optional<Card> findCard(String number, Short digit, Card.ExpirationDate expireAt) {
        var cardDoc = cards.find(findCardInMongo(number, digit, expireAt))
                .first();
        if (cardDoc == null)
            return Optional.empty();

        return Optional.of(deserializeCard(cardDoc));
    }

    @Override
    public Optional<Card> findCardByClient(ClientId clientId) {
        var clientDoc = clients.find(eq("_id", new ObjectId(clientId.getValue())))
                .first();
        if (clientDoc == null || clientDoc.get("card_id") == null)
            return Optional.empty();

        var cardDoc = cards.find(eq("_id", new ObjectId(clientDoc.get("card_id").toString())))
                .first();
        if (cardDoc == null)
            return Optional.empty();

        return Optional.of(deserializeCard(cardDoc));
    }

    @Override
    public void insertDefaultClientsWithCards() {
        upsertCard("5ff67f73deff4f00079b7f84", (short)333, "10/2025", 1_000_000_000.0);
        upsertCard("5ff873c1e77e950006a814af", (short)444, "01/2026", 1_500_000_000.0);
        upsertCard("5ff873cae77e950006a814b1", (short)555, "08/2027", 1_800_000_000.0);

        upsertClient("5ff867a5e77e950006a814ad", "5ff67f73deff4f00079b7f84");
        upsertClient("5ff878f5e77e950006a814b3", "5ff873c1e77e950006a814af");
        upsertClient("5ff87909e77e950006a814b5", "5ff873cae77e950006a814b1");
    }

    private void upsertCard(String number, Short digit, String expirationDate, Double limit) {
        cards.updateOne(eq("_id", new ObjectId(number)),
                combine(set("_id", new ObjectId(number)),
                        set("digit", digit),
                        set("expiration_date", expirationDate),
                        set("limit", limit)),
                optionsWithUpsert);
    }

    private void upsertClient(String clientId, String cardId) {
        clients.updateOne(eq("_id", new ObjectId(clientId)),
                combine(set("_id", new ObjectId(clientId)),
                        set("card_id", new ObjectId(cardId))),
                optionsWithUpsert);
    }

    private Document serializeCard(Card card) {
        return new Document("_id", new ObjectId(card.getNumber()))
                .append("digit", card.getDigit())
                .append("expiration_date", card.getExpirationDate().getValue())
                .append("limit", card.getLimit().getValue().doubleValue());
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
