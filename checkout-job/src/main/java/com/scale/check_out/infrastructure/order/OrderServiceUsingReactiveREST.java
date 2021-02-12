package com.scale.check_out.infrastructure.order;

import com.google.gson.Gson;
import com.scale.check_out.application.services.order.ConfirmOrderReactive;
import com.scale.check_out.application.services.order.ConvertShoppingCartReactive;
import com.scale.check_out.application.services.payment.PaymentDto;
import com.scale.check_out.infrastructure.configuration.SerializerConfig;
import com.scale.domain.CannotConfirmOrder;
import com.scale.domain.CannotCreateOrder;
import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

@RequiredArgsConstructor
@Slf4j
public class OrderServiceUsingReactiveREST implements ConvertShoppingCartReactive, ConfirmOrderReactive {
    @NonNull String serviceHost;
    @NonNull Integer servicePort;

    private final Gson gson = SerializerConfig.buildSerializer();
    private final HttpClient httpClient = initializeHttpClient();

    private HttpClient initializeHttpClient() {
        HttpClient client = HttpClient.create();
        client.warmup()
                .block();
        return client;
    }

    @Override
    public Mono<Order> intoOrder(ShoppingCart shoppingCart) {
        return httpClient.post()
                .uri(String.format("http://%s:%d/orders/convert", serviceHost, servicePort))
                .send(ByteBufFlux.fromString(Mono.just(gson.toJson(shoppingCart))))
                .responseSingle((httpClientResponse, byteBufMono) -> {
                    if (httpClientResponse.status() != HttpResponseStatus.CREATED)
                        return Mono.error(new CannotCreateOrder(String.format("Error %d creating order", httpClientResponse.status().code())));

                    return byteBufMono.asString();
                })
                .map(result -> gson.fromJson(result, Order.class));
    }

    @Override
    public Mono<Void> withPaymentReceipt(PaymentDto.PaymentReceiptDto receipt) {
        return httpClient.put()
                .uri(String.format("http://%s:%d/orders/%s/confirm", serviceHost, servicePort, receipt.getReference()))
                .send(ByteBufFlux.fromString(Mono.just(receipt.getNumber())))
                .responseSingle((httpClientResponse, byteBufMono) -> {
                    if (httpClientResponse.status() != HttpResponseStatus.NO_CONTENT)
                        return Mono.error(new CannotConfirmOrder(String.format("Error %d confirming order", httpClientResponse.status().code())));

                    return Mono.empty();
                });
    }

}
