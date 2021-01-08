package com.scale.domain;

import lombok.*;
import lombok.experimental.NonFinal;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Builder
@Value
public class Order {
    public static final String NO_ADDRESS = "";
    public static final String NO_PAYMENT_RECEIPT = "";
    public static final ZonedDateTime UNCONFIRMED = null;

    @NonNull OrderId id;
    @NonNull ZonedDateTime createdAt;
    @NonNull List<OrderItem> items;
    @NonFinal @Setter String fullAddress;
    @NonFinal String receipt;
    @NonFinal ZonedDateTime confirmedAt;

    public static Order with(String fullAddress, List<OrderItem> items) {
        return new Order(OrderId.generateNew(), ZonedDateTime.now(ZoneOffset.UTC), items, fullAddress, NO_PAYMENT_RECEIPT, UNCONFIRMED);
    }

    public Order(@NonNull OrderId id, @NonNull ZonedDateTime dateTime, @NonNull List<OrderItem> items,
                 @NonNull String fullAddress, String receipt, ZonedDateTime confirmedAt) {
        // TODO: Process discounts here
        if (items == null || items.isEmpty())
            throw new CannotCreateOrder("Cannot create order without items");
        if (fullAddress.isBlank())
            throw new CannotCreateOrder("Cannot create order without a delivery address");
        this.id = id;
        this.createdAt = dateTime;
        this.items = items;
        this.fullAddress = fullAddress;
        this.receipt = receipt;
        this.confirmedAt = confirmedAt;
    }

    public void confirm(String receipt) {
        if (items.isEmpty())
            throw new CannotConfirmOrder(String.format("Order %s does not have any items", this.id));
        if (fullAddress == null || fullAddress.isBlank())
            throw new CannotConfirmOrder(String.format("Order %s does not have a delivery address", this.id));
        if (receipt == null || receipt.isBlank())
            throw new CannotConfirmOrder(String.format("Cannot confirm order %s without a payment receipt", this.id));
        if (isConfirmed())
            throw new CannotConfirmOrder(String.format("Order %s is already confirmed", this.id));
        this.receipt = receipt;
        this.confirmedAt = ZonedDateTime.now(ZoneOffset.UTC);
    }

    public double calculateTotal() {
        return items.stream()
                .map(OrderItem::calculateTotal)
                .reduce(Double::sum)
                .orElse(0.0);
    }

    public boolean isConfirmed() {
        return (confirmedAt != null);
    }

    @Builder
    @Value
    public static class OrderItem {
        @NonNull String id;
        @NonNull Product product;
        @NonNull Integer quantity;

        public OrderItem(@NonNull String id, @NonNull Product product, @NonNull Integer quantity) {
            if (id.isBlank())
                throw new CannotCreateOrder("Order item 'id' cannot be blank");
            if (quantity <= 0)
                throw new CannotCreateOrder("Quantity must be greater than zero for item " + id);

            this.id = id;
            this.product = product;
            this.quantity = quantity;
        }

        public double calculateTotal() {
            return product.getPrice().doubleValue() * quantity;
        }
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
