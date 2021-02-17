package com.scale.check_out.application.controller;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.scale.check_out.application.services.CheckOutError;
import com.scale.check_out.application.usecases.PlaceOrder;
import com.scale.check_out.domain.metrics.BusinessMetrics;
import com.scale.check_out.infrastructure.configuration.SerializerConfig;
import com.scale.check_out.infrastructure.queue.RabbitMQChannelHandler;
import com.scale.domain.ShoppingCart;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@RequiredArgsConstructor
@Slf4j
public class ShoppingCartListener implements QueueConsumer {
    private final static String SHOPPING_CART_QUEUE = "shoppingcart.queue";

    private final Gson gson = SerializerConfig.buildSerializer();
    private String consumeTag = null;
    private Channel current = null;

    @NonNull RabbitMQChannelHandler queueManager;
    @NonNull BusinessMetrics metrics;
    @NonNull PlaceOrder placeOrder;

    @Override
    public void start() {
        if (consumeTag != null)
            throw new CheckOutError("Listener has already been started. Use method 'stop()' before re-starting.");
        try {
            this.current = queueManager.createChannel();
            this.current.queueDeclare(SHOPPING_CART_QUEUE, true, false, false, Collections.emptyMap());
            this.current.basicQos(100, true);
            consumeTag = this.current.basicConsume(SHOPPING_CART_QUEUE,
                    // Delivery callback
                    (s, delivery) -> {
                        try {
                            String content = new String(delivery.getBody(), StandardCharsets.UTF_8);

                            var shoppingCart = gson.fromJson(content, ShoppingCart.class);
                            metrics.newShoppingCartStarted(shoppingCart.getSessionId(), shoppingCart.getNumberOfClientsOnSameSession());

                            placeOrder.basedOn(shoppingCart);

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            this.current.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        }
                    },
                    // Cancel callback
                    s -> {
                        log.error("\t\tMsg was cancelled! \n" + s);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            this.current.basicCancel(consumeTag);
            consumeTag = null;
        } catch (IOException e) {
            throw new CheckOutError("Failure stopping channel: " + e.getMessage());
        }
    }

}
