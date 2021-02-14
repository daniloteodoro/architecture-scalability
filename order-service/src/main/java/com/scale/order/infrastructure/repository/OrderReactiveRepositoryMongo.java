package com.scale.order.infrastructure.repository;


import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.scale.domain.Order;
import com.scale.domain.Product;
import com.scale.order.application.usecases.OrderNotFound;
import com.scale.order.domain.repository.OrderReactiveRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class OrderReactiveRepositoryMongo implements OrderReactiveRepository {
    private final @NonNull MongoCollection<Document> orders;

    public OrderReactiveRepositoryMongo(@NonNull MongoDatabase db) {
        this.orders = db.getCollection("orders");
    }

    @Override
    public Mono<Order> store(Order order) {
        return Mono.from(orders.insertOne(serializeOrder(order)))
                .doOnSuccess(i -> log.info("Order {} was stored in Mongo", order.getId()))
                .thenReturn(order);
    }

    @Override
    public Mono<Order> update(Order order) {
        return Mono.from(orders.replaceOne(findById(order.getId()), serializeOrder(order)))
                .flatMap(updateResult -> {
                    if (updateResult.getMatchedCount() <= 0)
                        return Mono.error(new OrderNotFound("Could not find order to update: " + order.getId().value()));

                    log.info("Order {} was updated in Mongo", order.getId());
                    return Mono.just(order);
                });
    }

    @Override
    public Mono<Order> load(@NonNull Order.OrderId id) {
        return Mono.from(orders.find(findById(id)))
                .map(this::deserializeOrder);
    }

    private Document serializeOrder(Order order) {
        return new Document("_id", order.getId().value())
                .append("created_at", order.getCreatedAt().toString())
                .append("full_address", order.getFullAddress())
                .append("confirmed_at", order.getConfirmedAt() != null ? order.getConfirmedAt().toString() : null)
                .append("payment_receipt", order.getReceipt())
                .append("items", order.getItems().stream()
                        .map(this::serializeItem)
                        .collect(Collectors.toList()));
    }

    private Bson findById(Order.OrderId id) {
        return eq("_id", id.value());
    }

    private Document serializeItem(Order.OrderItem item) {
        return new Document()
                .append("item_id", item.getId())
                .append("product", serializeProduct(item.getProduct()))
                .append("quantity", item.getQuantity());
    }

    private Document serializeProduct(Product product) {
        return new Document()
                .append("product_id", product.getId())
                .append("name", product.getName())
                .append("in_stock", product.getInStock())
                .append("price", product.getPrice().doubleValue());
    }

    private Order deserializeOrder(Document doc) {
        var items = doc.getList("items", Document.class);
        return Order.builder()
                .id(Order.OrderId.of(doc.getString("_id")))
                .createdAt(ZonedDateTime.parse(doc.getString("created_at")))
                .fullAddress(doc.getString("full_address"))
                .confirmedAt(doc.get("confirmed_at") != null ? ZonedDateTime.parse(doc.getString("confirmed_at")) : null)
                .receipt(doc.getString("payment_receipt"))
                .items(items.stream()
                        .map(this::deserializeItem)
                        .collect(Collectors.toList()))
                .build();
    }

    private Order.OrderItem deserializeItem(Document doc) {
        return Order.OrderItem.builder()
                .id(doc.getString("item_id"))
                .product(deserializeProduct((Document) doc.get("product")))
                .quantity(doc.getInteger("quantity"))
                .build();
    }

    private Product deserializeProduct(Document doc) {
        return Product.builder()
                .id(doc.getLong("product_id"))
                .name(doc.getString("name"))
                .inStock(doc.getLong("in_stock"))
                .price(BigDecimal.valueOf(doc.getDouble("price")))
                .build();
    }

}
