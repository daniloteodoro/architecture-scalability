package com.scale.order.infrastructure.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.scale.domain.Order;
import com.scale.domain.Product;
import com.scale.order.application.usecases.OrderNotFound;
import com.scale.order.domain.repository.OrderRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class OrderRepositoryMongo implements OrderRepository {
    private final @NonNull MongoCollection<Document> orders;

    public OrderRepositoryMongo(@NonNull MongoDatabase db) {
        this.orders = db.getCollection("orders");
    }

    private Bson findById(Order.OrderId id) {
        return eq("_id", id.value());
    }

    @Override
    public void store(Order order) {
        orders.insertOne(serializeOrder(order));
        log.info("Order {} was stored in Mongo", order.getId());
    }

    private Document serializeOrder(Order order) {
        return new Document("_id", order.getId().value())
                .append("created_at", order.getCreatedAt().toString())
                .append("full_address", order.getFullAddress())
                .append("items", order.getItems().stream()
                        .map(this::serializeItem)
                        .collect(Collectors.toList()));
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

    @Override
    public void update(Order order) {
        if (orders.replaceOne(findById(order.getId()), serializeOrder(order)).getModifiedCount() <= 0)
            throw new OrderNotFound("Could not find order to update: " + order.getId().value());
        log.info("Order {} was updated in Mongo", order.getId());
    }

    @Override
    public Optional<Order> load(@NonNull Order.OrderId id) {
        var orderDoc = orders.find(findById(id))
                .first();
        if (orderDoc == null)
            return Optional.empty();

        return Optional.of(deserializeOrder(orderDoc));
    }

}
