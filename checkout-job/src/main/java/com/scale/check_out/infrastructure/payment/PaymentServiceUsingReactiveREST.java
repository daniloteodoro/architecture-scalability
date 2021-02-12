package com.scale.check_out.infrastructure.payment;

import com.google.gson.Gson;
import com.scale.check_out.application.services.payment.PayOrderReactive;
import com.scale.check_out.application.services.payment.PaymentDto;
import com.scale.check_out.infrastructure.configuration.SerializerConfig;
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
public class PaymentServiceUsingReactiveREST implements PayOrderReactive {
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
    public Mono<PaymentDto.PaymentReceiptDto> with(ShoppingCart.ClientId clientId, Order order) {
        var paymentRequest = new PaymentDto.PaymentRequestDto(order.calculateTotal(), order.getId().value(), clientId);

        return httpClient.post()
                .uri(String.format("http://%s:%d/payments", serviceHost, servicePort))
                .send(ByteBufFlux.fromString(Mono.just(gson.toJson(paymentRequest))))
                .responseSingle((httpClientResponse, byteBufMono) -> {
                    if (httpClientResponse.status() != HttpResponseStatus.CREATED && httpClientResponse.status() != HttpResponseStatus.OK)
                        return Mono.error(new CannotCreateOrder(String.format("Error %d paying order", httpClientResponse.status().code())));
                    else
                        return byteBufMono.asString();
                })
                .map(result -> gson.fromJson(result, PaymentDto.PaymentReceiptDto.class));
    }

}
