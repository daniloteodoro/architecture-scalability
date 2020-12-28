package com.scale.management.infrastructure.shoppingcart;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.rabbitmq.client.Channel;
import com.scale.domain.ShoppingCart;
import com.scale.management.domain.model.ShoppingCartPublisher;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Objects;

/**
 * Adapter class for the ShoppingCartPublisher, implemented using AMQP
 */
public class ShoppingCartPublisherUsingAMQP implements ShoppingCartPublisher {
    private final static String NO_EXCHANGE = "";
    private final static String SHOPPING_CART_QUEUE = "shoppingcart.queue";

    private final Channel channel;
    private final static Gson serializer = buildSerializer();

    private static Gson buildSerializer() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .registerTypeAdapter(ZonedDateTime.class, new TypeAdapter<ZonedDateTime>() {
                    @Override
                    public void write(JsonWriter out, ZonedDateTime value) throws IOException {
                        out.value(value.toString());
                    }
                    @Override
                    public ZonedDateTime read(JsonReader in) throws IOException {
                        return ZonedDateTime.parse(in.nextString());
                    }
                })
                .enableComplexMapKeySerialization()
                .create();
    }

    public ShoppingCartPublisherUsingAMQP(Channel queueChannel) throws IOException {
        this.channel = Objects.requireNonNull(queueChannel, "Queue channel is mandatory");
        queueChannel.queueDeclare(SHOPPING_CART_QUEUE, true, false, false, Collections.emptyMap());
    }

    @Override
    public void publish(ShoppingCart shoppingCart) {
        try {
            this.channel.basicPublish(
                    NO_EXCHANGE,
                    SHOPPING_CART_QUEUE,
                    null,
                    serializer.toJson(shoppingCart).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
