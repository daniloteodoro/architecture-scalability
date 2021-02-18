package com.scale.payment.infrastructure.repository;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentReactiveRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Objects;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@Slf4j
public class PaymentReactiveRepositoryMongo implements PaymentReactiveRepository {
    private final UpdateOptions optionsWithUpsert = new UpdateOptions().upsert(true);
    private final TransactionOptions txnOptions = TransactionOptions.builder()
            .readPreference(ReadPreference.primary())
            .readConcern(ReadConcern.SNAPSHOT)
            .writeConcern(WriteConcern.MAJORITY)
            .build();

    private final @NonNull MongoCollection<Document> clients;
    private final @NonNull MongoCollection<Document> cards;
    private final @NonNull MongoCollection<Document> receipts;

    public PaymentReactiveRepositoryMongo(@NonNull MongoDatabase db) {
        this.cards = db.getCollection("cards");
        this.receipts = db.getCollection("receipts");
        this.clients = db.getCollection("clients");
    }

    @Override
    public Mono<Card.Receipt> findReceipt(Order.OrderId id, Money amount) {
        return Mono.from(receipts.find(findReceiptInMongo(id, amount)))
                .flatMap(this::deserializePaymentReceipt);
    }

    @Override
    public Mono<Card.Receipt> addReceipt(Card.Receipt receipt) {
        // Waiting for improved transaction support https://jira.mongodb.org/browse/JAVA-3539
        return Mono.from(receipts.insertOne(serializePaymentReceipt(receipt)))
                .then(Mono.from(cards.updateOne(eq("_id", new ObjectId(receipt.getCard().getNumber())),
                        set("limit", receipt.getCard().getLimit().getValue().doubleValue()))))
                .doOnSuccess(i -> log.info("Receipt {} with reference to {} was stored in Mongo", receipt.getNumber(), receipt.getReference()))
                .thenReturn(receipt);
    }

    @Override
    public Mono<Void> addCard(Card card) {
        return Mono.from(cards.insertOne(serializeCard(card)))
                .doOnSuccess(i -> log.info("Card {} was stored in Mongo", card.getNumber()))
                .cast(Void.class);
    }

    @Override
    public Mono<Card> findCard(String number, Short digit, Card.ExpirationDate expireAt) {
        return Mono.from(cards.find(findCardInMongo(number, digit, expireAt)))
                .map(this::deserializeCard);
    }

    @Override
    public Mono<Card> findCardByClient(ClientId clientId) {
        return Mono.from(clients.find(eq("_id", new ObjectId(clientId.getValue()))))
                .filter(Objects::nonNull)
                .flatMap(clientDoc -> findCard(clientDoc.getObjectId("card_id")));
    }

    @Override
    public void insertDefaultClientsWithCards() {
        Flux.concat(
                upsertCard("5ff67f73deff4f00079b7f84", (short)333, "10/2025", 1_000_000_000.0),
                upsertCard("5ff873c1e77e950006a814af", (short)444, "01/2026", 1_500_000_000.0),
                upsertCard("5ff873cae77e950006a814b1", (short)555, "08/2027", 1_800_000_000.0),
                upsertClient("5ff867a5e77e950006a814ad", "5ff67f73deff4f00079b7f84"),
                upsertClient("5ff878f5e77e950006a814b3", "5ff873c1e77e950006a814af"),
                upsertClient("5ff87909e77e950006a814b5", "5ff873cae77e950006a814b1")
            )
            .log()
            .subscribe();
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

    private Publisher<UpdateResult> upsertCard(String number, Short digit, String expirationDate, Double limit) {
        return cards.updateOne(eq("_id", new ObjectId(number)),
                combine(set("_id", new ObjectId(number)),
                        set("digit", digit),
                        set("expiration_date", expirationDate),
                        set("limit", limit)),
                optionsWithUpsert);
    }

    private Publisher<UpdateResult> upsertClient(String clientId, String cardId) {
        return clients.updateOne(eq("_id", new ObjectId(clientId)),
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

    private Mono<Card> findCard(ObjectId cardId) {
        return Mono.from(cards.find(eq("_id", cardId)))
                .map(this::deserializeCard);
    }

    private Mono<Card.Receipt> deserializePaymentReceipt(Document doc) {
        return Mono.from(findCard(doc.getObjectId("card_id")))
                .filter(Objects::nonNull)
                .map(associatedCard -> associatedCard.regenerateReceipt(doc.getString("_id"),
                        ZonedDateTime.parse(doc.getString("time")),
                        Money.of(doc.getDouble("amount")),
                        doc.getString("reference")))
                .switchIfEmpty(Mono.error(new Card.CardError("Card was not found: " + doc.getObjectId("card_id").toString())));
    }
}
