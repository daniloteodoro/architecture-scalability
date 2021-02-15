package com.scale.payment.application.controller;

import com.google.gson.Gson;
import com.scale.domain.Order;
import com.scale.payment.application.usecases.PayOrderReactive;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.model.PaymentError;
import com.scale.payment.infrastructure.configuration.SerializerConfig;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
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
        return getPaymentRequestFromPayload(request.receive().aggregate().asString())
                .flatMap(this::validateAndPay)
                .flatMap(receipt -> {
                    var status = (receipt instanceof Card.OrderAlreadyPaidReceipt) ? HttpResponseStatus.OK : HttpResponseStatus.CREATED;
                    log.info("Order {} was paid using Reactive REST", receipt.getReference());
                    return Mono.from(response.status(status)
                            .header("location", "/receipts/" + receipt.getNumber())
                            .header("Content-Type", "application/json")
                            .sendString(Mono.just(gson.toJson(PaymentReceiptDto.from(receipt)))));
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    if (e instanceof Card.CardError)
                        return Mono.from(response.status(HttpResponseStatus.PAYMENT_REQUIRED)
                                .sendString(Mono.just(e.getMessage()).log()));
                    else if (e instanceof PaymentError)
                        return Mono.from(response.status(HttpResponseStatus.BAD_REQUEST)
                                .sendString(Mono.just(e.getMessage()).log()));
                    else
                        return Mono.from(response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                                .sendString(Mono.just("Failure processing payment").log()));
                });
    }

    private Mono<Card.Receipt> validateAndPay(PaymentRequestDto requestDto) {
        if (requestDto == null || !requestDto.isValid())
            return Mono.error(new PaymentError("Payload containing the full payment request is mandatory"));
        if (requestDto.getOrderId().isBlank())
            return Mono.error(new PaymentError("Order id is mandatory"));
        if (requestDto.getAmount() <= 0d)
            return Mono.error(new PaymentError("Amount must be greater than zero"));
        if (!requestDto.getClientId().isValid())
            return Mono.error(new PaymentError("Client id is mandatory"));

        return payOrder.usingClientCard(requestDto.clientId, Order.OrderId.of(requestDto.orderId), Money.of(requestDto.amount));
    }

    // REST-related methods
    private Mono<PaymentRequestDto> getPaymentRequestFromPayload(Mono<String> payload) {
        return payload.flatMap(content -> {
            try {
                if (content.isBlank())
                    return Mono.error(new PaymentError("Payment request is mandatory"));

                return Mono.just(gson.fromJson(content, PaymentRequestDto.class));
            } catch (Exception e) {
                log.error("Json parsing failure.\n" + content);
                return Mono.error(e);
            }
        })
        .switchIfEmpty(Mono.error(new PaymentError("Payment request is mandatory")));
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
