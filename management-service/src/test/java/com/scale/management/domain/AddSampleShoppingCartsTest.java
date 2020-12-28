package com.scale.management.domain;

import com.scale.domain.ShoppingCart;
import com.scale.management.domain.usecases.AddSampleShoppingCarts;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AddSampleShoppingCartsTest {

    @Test
    public void whenHandlingAddShoppingCartCommand_Then5CartsGetCreated() {
        AtomicInteger cartsAdded = new AtomicInteger();
        var addSampleShoppingCarts = new AddSampleShoppingCarts(shoppingCart -> cartsAdded.getAndIncrement());
        var command = AddSampleShoppingCarts.AddShoppingCartCommand.builder()
                .number(5)
                .maxNumberOfClients(5)
                .ackFirst(true)
                .ackLast(true)
                .build();
        addSampleShoppingCarts.handle(command);
        assertThat(cartsAdded.get(), is(equalTo(5)));
    }

    @Test
    public void whenAddingOnly1ShoppingCart_ThenResultBecomesBothFirstAndLast() {
        AtomicReference<ShoppingCart> cartAdded = new AtomicReference<>(null);
        var addSampleShoppingCarts = new AddSampleShoppingCarts(cartAdded::set);
        var command = AddSampleShoppingCarts.AddShoppingCartCommand.builder()
                .number(1)
                .maxNumberOfClients(1)
                .ackFirst(true)
                .ackLast(true)
                .build();
        addSampleShoppingCarts.handle(command);
        assertThat(cartAdded.get(), is(notNullValue()));
        assertThat(cartAdded.get().getIsFirst(), is(true));
        assertThat(cartAdded.get().getIsLast(), is(true));
    }

}
