package com.scale.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Builder
@Value
public class ShoppingCart {

    @NonNull String sessionId;
    @NonNull ZonedDateTime createdAt;
    @NonNull ClientId clientId;
    // TODO: Add a class to hold addresses
    String address;
    String zipCode;
    String city;
    String state;
    String country;
    @NonNull List<ShoppingCartItem> items;
    // Metadata
    @NonNull Long numberOfClientsOnSameSession;
    @NonNull Boolean isFirst;
    @NonNull Boolean isLast;

    public Order convert() {
        if (getItems().isEmpty())
            throw new CannotConvertShoppingCart("Cannot convert an empty shopping cart to an order");
        var orderItems = getItems().stream()
                .map(this::toOrderItem)
                .collect(Collectors.toList());
        return Order.with(generateFullAddress(), orderItems);
    }

    private String generateFullAddress() {
        return String.format("%s, %s, %s, %s, %s", address, zipCode, city, state, country);
    }

    private Order.OrderItem toOrderItem(ShoppingCartItem input) {
        return Order.OrderItem.builder()
                .id(input.getId())  // ???
                .product(input.product)
                .quantity(input.quantity)
                .build();
    }

    @Builder
    @Value
    public static class ShoppingCartItem {
        @NonNull String id;
        @NonNull Product product;
        @NonNull Integer quantity;
    }

    @Value
    public static class ClientId {
        @NonNull String value;

        private ClientId(String value) {
            this.value = Objects.requireNonNull(value, "ClientId's value is mandatory");
        }
        public static ClientId of(String id) {
            return new ClientId(id);
        }

        public String value() {
            return value;
        }
    }

}
