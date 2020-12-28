package com.scale.management.domain.usecases;

import com.scale.domain.Product;
import com.scale.domain.ShoppingCart;
import com.scale.management.domain.model.CannotBuildSampleShoppingCart;
import com.scale.management.domain.model.ShoppingCartPublisher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
public class AddSampleShoppingCarts {
    @NonNull ShoppingCartPublisher shoppingCartPublisher;

    public String handle(AddShoppingCartCommand addShoppingCartCommand) {
        log.info("Started sending messages to the queue - " + addShoppingCartCommand.number);
        String newSession = addShoppingCartCommand.hasSession() ? addShoppingCartCommand.session : startNewSession();
        for (int i = 0; i < addShoppingCartCommand.number; i++) {
            boolean ackFirst = false;
            boolean ackLast = false;
            if (i == 0) {
                ackFirst = addShoppingCartCommand.ackFirst;
                ackLast = addShoppingCartCommand.number == 1 && addShoppingCartCommand.ackLast;
            } else if (i == (addShoppingCartCommand.number - 1)) {
                ackLast = addShoppingCartCommand.ackLast;
            }
            shoppingCartPublisher.publish(generateSampleShoppingCart(newSession, addShoppingCartCommand.maxNumberOfClients, ackFirst, ackLast));
        }
        log.info("All messages sent to the queue");
        return newSession;
    }

    private ShoppingCart generateSampleShoppingCart(String sessionId, Long numberOfCustomers, boolean ackFirst, boolean ackLast) {
        try {
            var firstProduct = Product.builder()
                    .id(7L)
                    .name("First Product")
                    .price(BigDecimal.valueOf(3.5))
                    .inStock(150L)
                    .build();
            var secondProduct = Product.builder()
                    .id(55L)
                    .name("Second product")
                    .price(BigDecimal.valueOf(198.98))
                    .inStock(3L)
                    .build();
            var items = Arrays.asList(
                    ShoppingCart.ShoppingCartItem.builder()
                            .id("6")
                            .product(firstProduct)
                            .quantity(2)
                            .build(),
                    ShoppingCart.ShoppingCartItem.builder()
                            .id("12")
                            .product(secondProduct)
                            .quantity(1)
                            .build()
            );

            return ShoppingCart.builder()
                    .sessionId(sessionId)
                    .createdAt(ZonedDateTime.now(ZoneOffset.UTC))
                    .clientId(ShoppingCart.ClientId.of("5"))
                    .address("33 street, 5")
                    .zipCode("1234")
                    .city("Rotterdam")
                    .state("ZH")
                    .country("NL")
                    .items(items)
                    .numberOfClientsOnSameSession(numberOfCustomers)
                    .isFirst(ackFirst)
                    .isLast(ackLast)
                    .build();
        } catch (Exception e) {
            log.error("Failure generating sample shopping cart template: "+e.getMessage());
            throw new CannotBuildSampleShoppingCart("Failure generating sample shopping cart template");
        }
    }

    public String startNewSession() {
        return UUID.randomUUID().toString();
    }

    @Builder
    @Value
    public static class AddShoppingCartCommand {
        long number;
        @NonNull @Builder.Default
        String session = "";
        boolean ackFirst;
        boolean ackLast;
        long maxNumberOfClients;

        public boolean hasSession() {
            return !this.session.isBlank();
        }
    }

}
