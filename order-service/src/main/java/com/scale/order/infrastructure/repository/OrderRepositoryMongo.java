package com.scale.order.infrastructure.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.scale.domain.Order;
import com.scale.order.application.usecase.OrderNotFound;
import com.scale.order.domain.repository.OrderRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;

import java.util.Optional;
import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class OrderRepositoryMongo implements OrderRepository {
    private final @NonNull MongoCollection<Order> orders;

    public OrderRepositoryMongo(@NonNull MongoDatabase db) {
        this.orders = db.getCollection("orders", Order.class);
    }

    @Override
    public void store(Order order) {
        orders.insertOne(order);
        log.info("Order {} was stored in Mongo", order.getId());
    }

    private Bson findById(Order.OrderId id) {
        return eq("_id.value", id.value());
    }

    @Override
    public void update(Order order) {
        if (orders.replaceOne(findById(order.getId()), order).getModifiedCount() <= 0)
            throw new OrderNotFound("Could not find order to update: " + order.getId().value());
        log.info("Order {} was updated in memory", order.getId());
    }

    @Override
    public Optional<Order> load(@NonNull Order.OrderId id) {
        var possibleOrder = orders.find(findById(id))
                .first();
        return Optional.ofNullable(possibleOrder);
    }
}
