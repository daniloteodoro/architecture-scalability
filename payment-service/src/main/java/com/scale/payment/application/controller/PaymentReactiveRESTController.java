package com.scale.payment.application.controller;

import com.google.gson.Gson;
import com.scale.domain.Order;
import com.scale.payment.application.usecases.PayOrderReactive;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.model.PaymentError;
import com.scale.payment.infrastructure.configuration.SerializerConfig;
import kong.unirest.HttpStatus;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.time.ZonedDateTime;

@RequiredArgsConstructor
@Slf4j
public class PaymentReactiveRESTController {
    @NonNull PayOrderReactive payOrder;

    private final Gson gson = SerializerConfig.buildSerializer();

    public Publisher<Void> handlePayment(HttpServerRequest request, HttpServerResponse response) {
        return getPaymentRequestFromPayload(request.receive().asString())
                .flatMap(paymentRequest -> validateAndPay(response, paymentRequest))
                .switchIfEmpty(response.status(HttpStatus.BAD_REQUEST));
    }

    private Publisher<Void> validateAndPay(HttpServerResponse response, PaymentRequestDto requestDto) {
        if (requestDto == null || !requestDto.isValid()) {
            return response.status(HttpStatus.BAD_REQUEST)
                    .sendString(Mono.just("Payload containing the full payment request is mandatory").log());
        }
        if (requestDto.getOrderId().isBlank()) {
            return response.status(HttpStatus.BAD_REQUEST)
                    .sendString(Mono.just("Order id is mandatory").log());
        }
        if (requestDto.getAmount() <= 0d) {
            return response.status(HttpStatus.BAD_REQUEST)
                    .sendString(Mono.just("Amount must be greater than zero").log());
        }
        if (!requestDto.getClientId().isValid()) {
            return response.status(HttpStatus.BAD_REQUEST)
                    .sendString(Mono.just("Client id is mandatory").log());
        }

        return pay(response, requestDto.clientId, requestDto.orderId, requestDto.amount)
                .switchIfEmpty(Mono.from(response.status(HttpStatus.NOT_FOUND).
                        sendString(Mono.just(String.format("Client %s was not found or has no associated card", requestDto.clientId.getValue())))));
    }

    private Mono<Void> pay(HttpServerResponse response, ClientId card, String orderId, Double amount) {
        // Pay order with id 123, total 342.09 with card number 9999 from client X
        return payOrder.usingClientCard(card, Order.OrderId.of(orderId), Money.of(amount))
                .flatMap(receipt -> {
                    var status = (receipt instanceof Card.OrderAlreadyPaidReceipt) ? HttpStatus.OK : HttpStatus.CREATED;
                    log.info("Order {} was paid using Reactive REST", orderId);
                    return Mono.from(response.status(status)
                            .header("location", "/receipts/" + receipt.getNumber())
                            .header("Content-Type", "application/json")
                            .sendString(Mono.just(gson.toJson(PaymentReceiptDto.from(receipt)))));
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    if (e instanceof PaymentError)
                        return Mono.from(response.status(HttpStatus.PAYMENT_REQUIRED)
                                .sendString(Mono.just(e.getMessage()).log()));
                    else
                        return Mono.from(response.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .sendString(Mono.just("Failure processing payment").log()));
                });
    }

    // REST-related methods
    private Flux<PaymentRequestDto> getPaymentRequestFromPayload(Flux<String> payload) {
        return payload.map(content -> {
            try {
                log.info(content);
                return gson.fromJson(content, PaymentRequestDto.class);
            } catch (Exception e) {
                log.error("Json parsing failure.\n" + content);
                throw e;
            }
        });
    }

    // TODO: Can be shared among REST controllers
    @Value
    private static class PaymentRequestDto {
        @NonNull Double amount;
        @NonNull String orderId;
        @NonNull ClientId clientId;

        public boolean isValid() {
            return (amount != null &&
                    orderId != null &&
                    clientId != null);
        }
    }

    @Value
    private static class CardDetailsDto {
        @NonNull String number;
        @NonNull Short digit;
        // MM/YYYY
        @NonNull String expirationDate;
    }

    @Value
    public static class PaymentReceiptDto {
        @NonNull String number;
        @NonNull ZonedDateTime time;
        @NonNull String reference;
        @NonNull Double amount;

        public static PaymentReceiptDto from(Card.Receipt receipt) {
            return new PaymentReceiptDto(receipt.getNumber(), receipt.getTime(), receipt.getReference(), receipt.getAmount().getValue().doubleValue());
        }
    }

}
