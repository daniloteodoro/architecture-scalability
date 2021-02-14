package com.scale.order.application.controller;

import com.google.gson.Gson;
import com.scale.domain.DomainError;
import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;
import com.scale.order.application.exceptions.OrderError;
import com.scale.order.application.usecases.ConfirmOrderReactive;
import com.scale.order.application.usecases.GenerateOrderReactive;
import com.scale.order.domain.model.PaidOrder;
import com.scale.order.infrastructure.configuration.SerializerConfig;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

@RequiredArgsConstructor
@Slf4j
public class OrderManagementReactiveRESTController {
    @NonNull GenerateOrderReactive generateOrder;
    @NonNull ConfirmOrderReactive confirmOrder;

    private final Gson gson = SerializerConfig.buildSerializer();

    public Publisher<Void> handleCreateOrder(HttpServerRequest request, HttpServerResponse response) {
        return getShoppingCartFromDto(request.receive().aggregate().asString())
                .flatMap(shoppingCart -> generateOrder.fromShoppingCart(shoppingCart))
                .flatMap(order -> Mono.from(response.status(HttpResponseStatus.CREATED)
                            .header("location", "/orders/" + order.getId())
                            .header("Content-Type", "application/json")
                            .sendString(Mono.just(gson.toJson(order)))))
                .onErrorResume(throwable -> {
                    if ((throwable instanceof OrderError || throwable instanceof DomainError))
                        return Mono.from(response.status(HttpResponseStatus.BAD_REQUEST)
                                .sendString(Mono.just(throwable.getMessage())));
                    else
                        return Mono.from(response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                                .sendString(Mono.just("Failure converting this shopping cart into an order.")));
                });
    }

    public Publisher<Void> handleConfirm(HttpServerRequest request, HttpServerResponse response) {
        var paramId = request.param("id");
        if (paramId == null || paramId.isBlank()) {
            return response.status(HttpResponseStatus.BAD_REQUEST)
                    .sendString(Mono.just("Order id is mandatory"));
        }
        Order.OrderId orderId = Order.OrderId.of(paramId);

        return getPaidOrderFromPayload(request.receive().aggregate().asString(), orderId)
                .flatMap(confirmOrder::withPaymentReceipt)
                .flatMap(order -> {
                    log.info("Order {} was confirmed using Reactive REST", orderId);
                    return Mono.from(response.status(HttpResponseStatus.NO_CONTENT)
                            .sendString(Mono.empty()));
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    if ((e instanceof OrderError || e instanceof DomainError))
                        return Mono.from(response.status(HttpResponseStatus.BAD_REQUEST)
                                .sendString(Mono.just(e.getMessage()).log()));
                    else
                        return Mono.from(response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                                .sendString(Mono.just("Unknown error while confirming order " + orderId).log()));
                });
    }

    // REST-related methods
    private Mono<PaidOrder> getPaidOrderFromPayload(Mono<String> payload, Order.OrderId orderId) {
        return payload.flatMap(content -> {
            if (content.isBlank())
                return Mono.error(new OrderError("Receipt number is mandatory"));

            return Mono.just(PaidOrder.of(orderId, content));
        })
        .switchIfEmpty(Mono.error(new OrderError("Receipt number is mandatory")));
    }

    private Mono<ShoppingCart> getShoppingCartFromDto(Mono<String> payload) {
        return payload.flatMap(content -> {
            try {
                if (content.isBlank())
                    return Mono.error(new OrderError("Shopping cart payload is mandatory"));

                return Mono.just(gson.fromJson(content, ShoppingCart.class));
            } catch (Exception e) {
                log.error("Json parsing failure.\n" + content);
                return Mono.error(e);
            }
        })
        .switchIfEmpty(Mono.error(new OrderError("Shopping cart payload is mandatory")));
    }

}
