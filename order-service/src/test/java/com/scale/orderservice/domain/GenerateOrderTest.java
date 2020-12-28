package com.scale.orderservice.domain;

import com.scale.domain.CannotConvertShoppingCart;
import com.scale.domain.Product;
import com.scale.domain.ShoppingCart;
import com.scale.order.domain.model.GenerateOrder;
import com.scale.order.infrastructure.repository.OrderRepositoryInMemory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GenerateOrderTest {

    @Test
    public void givenAShoppingCartWhenConvertToOrderThenAnOrderIsGenerated() {
        var repo = new OrderRepositoryInMemory();
        var generateOrder = new GenerateOrder(repo);
        var item = ShoppingCart.ShoppingCartItem.builder()
                .id("1")
                .product(Product.builder()
                        .id(15L)
                        .name("First")
                        .price(BigDecimal.valueOf(12.3))
                        .inStock(18L)
                        .build())
                .quantity(2)
                .build();
        var cart = ShoppingCart.builder()
                .sessionId("1")
                .createdAt(ZonedDateTime.now(ZoneOffset.UTC))
                .clientId(ShoppingCart.ClientId.of("1"))
                .numberOfClientsOnSameSession(1L)
                .isFirst(true)
                .isLast(true)
                .items(Collections.singletonList(item))
                .build();

        var resultingOrder = generateOrder.fromShoppingCart(cart);

        assertNotNull(resultingOrder);
        assertThat(resultingOrder.getId().value().length(), is(greaterThan(10)));
        assertThat(resultingOrder.getItems().size(), is(greaterThan(0)));
    }

    @Test
    public void givenAnEmptyShoppingCartWhenConvertToOrderThenAnErrorIsThrown() {
        AtomicReference<ShoppingCart> cartAdded = new AtomicReference<>(null);
        var repo = new OrderRepositoryInMemory();
        var generateOrder = new GenerateOrder(repo);
        var cart = ShoppingCart.builder()
                .sessionId("1")
                .createdAt(ZonedDateTime.now(ZoneOffset.UTC))
                .clientId(ShoppingCart.ClientId.of("1"))
                .numberOfClientsOnSameSession(1L)
                .isFirst(true)
                .isLast(true)
                .items(new ArrayList<>())
                .build();
        assertThrows(CannotConvertShoppingCart.class, () -> generateOrder.fromShoppingCart(cart));
    }

}
