package com.scale.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Builder
@Value
public class Order {

    @NonNull OrderId id;
MongoDb: instruct mongo to serialize it in a simpler way e.g. ISO format, see localhost:8081
    @NonNull ZonedDateTime createdAt;
    @NonNull List<OrderItem> items;

    public static Order with(ZonedDateTime dateTime, List<OrderItem> items) {
        // TODO: Process discounts here
        if (items.isEmpty())
            throw new CannotCreateOrder("Cannot create order without items");
        return new Order(OrderId.generateNew(), dateTime, items);
    }

    @Builder
    @Value
    public static class OrderItem {
        @NonNull String id;
        @NonNull Product product;
        @NonNull Integer quantity;
    }

    @Value
    public static class OrderId {
        @NonNull String value;

        private OrderId(String value) {
            this.value = Objects.requireNonNull(value, "OrderId's value is mandatory");
        }
        public static OrderId generateNew() {
            return new OrderId(UUID.randomUUID().toString());
        }
        public static OrderId of(String value) {
            return new OrderId(value);
        }

        public String value() {
            return value;
        }
    }

}
