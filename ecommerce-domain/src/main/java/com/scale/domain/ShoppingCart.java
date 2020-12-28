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

    public static ShoppingCart forClient(ClientId clientId, String sessionId, ZonedDateTime createdAt, Long maxNumberOfClients,
                                         Boolean isFirst, Boolean isLast, List<ShoppingCartItem> items) {
        if (clientId == null)
            throw new InvalidClient("Client is mandatory");
        if (sessionId == null || sessionId.isBlank())
            throw new InvalidSession("Session cannot be blank");
        return ShoppingCart.builder()
                .sessionId(sessionId)
                .createdAt(createdAt)
                .clientId(clientId)
                .numberOfClientsOnSameSession(maxNumberOfClients)
                .isFirst(isFirst)
                .isLast(isLast)
                .items(items)
                .build();
    }

    public Order convert() {
        if (getItems().isEmpty())
            throw new CannotConvertShoppingCart("Cannot convert empty shopping cart to an order");
        var orderItems = getItems().stream()
                .map(this::toOrderItem)
                .collect(Collectors.toList());
        return Order.with(ZonedDateTime.now(ZoneOffset.UTC), orderItems);
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
