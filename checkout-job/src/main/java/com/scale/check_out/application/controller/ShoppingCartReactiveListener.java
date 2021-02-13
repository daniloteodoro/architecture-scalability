package com.scale.check_out.application.controller;

import com.google.gson.Gson;
import com.scale.check_out.application.services.CheckOutError;
import com.scale.check_out.application.usecases.PlaceOrderReactive;
import com.scale.check_out.domain.metrics.BusinessMetrics;
import com.scale.check_out.infrastructure.configuration.SerializerConfig;
import com.scale.check_out.infrastructure.queue.RabbitMQChannelHandler;
import com.scale.domain.ShoppingCart;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Slf4j
public class ShoppingCartReactiveListener implements QueueConsumer {
    private final static String SHOPPING_CART_QUEUE = "shoppingcart.queue";

    @NonNull RabbitMQChannelHandler queueManager;
    @NonNull BusinessMetrics metrics;
    @NonNull PlaceOrderReactive placeOrder;

    private final Gson gson = SerializerConfig.buildSerializer();
    private String consumeTag = null;

    @Override
    public Flux<Void> start() {
        if (consumeTag != null)
            throw new CheckOutError("Listener has already been started. Use method 'stop()' before re-starting.");

        queueManager.declareQueueNonBlocking(SHOPPING_CART_QUEUE)
                .subscribe();

        return queueManager.createNonBlockingReceiver()
                .consumeAutoAck(SHOPPING_CART_QUEUE)
                .flatMap(delivery -> {
                    try {
                        String content = new String(delivery.getBody(), StandardCharsets.UTF_8);
                        var shoppingCart = gson.fromJson(content, ShoppingCart.class);
                        metrics.newShoppingCartStarted(shoppingCart.getSessionId(), shoppingCart.getNumberOfClientsOnSameSession());
                        return placeOrder.basedOn(shoppingCart);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Mono.empty();
                    }
                })
                .onErrorResume(throwable -> Mono.empty());
    }

    @Override
    public void stop() {
        try {
//            receiver.basicCancel(consumeTag);
            consumeTag = null;
        } catch (Exception e) {
            throw new CheckOutError("Failure stopping channel: " + e.getMessage());
        }
    }

}
