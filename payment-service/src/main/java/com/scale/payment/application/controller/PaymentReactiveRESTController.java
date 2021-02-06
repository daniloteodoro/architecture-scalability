package com.scale.payment.application.controller;

import java.time.ZonedDateTime;
import com.google.gson.Gson;
import com.scale.domain.Order;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.model.PaymentError;
import com.scale.payment.domain.repository.PaymentReactiveRepository;
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

@RequiredArgsConstructor
@Slf4j
public class PaymentReactiveRESTController {
    @NonNull PayOrder payOrder;
    @NonNull PaymentReactiveRepository paymentRepository;

    private final Gson gson = SerializerConfig.buildSerializer();

    public Publisher<Void> handlePayment(HttpServerRequest request, HttpServerResponse response) {
//        var requestDto = getPaymentRequestFromPayload(request.receive().asString())
//                .map(dto -> process(response, dto));

        return getPaymentRequestFromPayload(request.receive().asString())
                .flatMap(dto -> validateAndPay(response, dto))
                .switchIfEmpty(response.status(HttpStatus.BAD_REQUEST));

//        var requestDto = getPaymentRequestFromPayload(request.body);
//
//        if (requestDto.getOrderId() == null || requestDto.getOrderId().isBlank()) {
//            return response.status(HttpStatus.BAD_REQUEST)
//                    .sendString(Mono.just("Order id is mandatory").log());
//        }
//        if (requestDto.getAmount() <= 0d) {
//            return response.status(HttpStatus.BAD_REQUEST)
//                    .sendString(Mono.just("Amount must be greater than zero").log());
//        }
//        if (requestDto.getClientId() == null) {
//            return response.status(HttpStatus.BAD_REQUEST)
//                    .sendString(Mono.just("Client id is mandatory").log());
//        }

//        var requestedCard = paymentRepository.findCardByClient(requestDto.clientId);
//        if (requestedCard.isEmpty()) {
//            return ServerResponse.notFound()
//                    .header("reason", String.format("Client %s was not found or has no associated card", requestDto.clientId.getValue()))
//                    .build();
//        }
//
//        // Pay order with id 123, total 342.09 with card number 9999 from client X
//        Card.Receipt receipt;
//        try {
//            receipt = payOrder.using(requestedCard.get(), Order.OrderId.of(requestDto.getOrderId()), Money.of(requestDto.getAmount()));
//        } catch (PaymentError e) {
//            e.printStackTrace();
//            return ServerResponse.status(HttpStatus.PAYMENT_REQUIRED_402)
//                    .bodyValue(e.getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
//                    .bodyValue("Failure processing payment");
//        }
//
//        log.info("Order {} was paid using Reactive REST", requestDto.getOrderId());
//
//        var status = (receipt instanceof Card.OrderAlreadyPaidReceipt) ? HttpStatus.OK_200 : HttpStatus.CREATED_201;
//
//        context.header("location", "/receipts/" + receipt.getNumber())
//                .json(PaymentReceiptDto.from(receipt))
//                .status(status);
//        return ServerResponse.status(status)
//                .header("location", "/receipts/" + receipt.getNumber())
//                .body(Mono.just(PaymentReceiptDto.from(receipt)));

//        return response.status(201)
//                .addHeader("sample", "value of the sample")
//                .sendString(Mono.just("Yeeep")
//                        .log());
    }

    private Publisher<Void> validateAndPay(HttpServerResponse response, PaymentRequestDto requestDto) {
        if (requestDto == null) {
            return response.status(HttpStatus.BAD_REQUEST)
                    .sendString(Mono.just("Payload containing the payment request is mandatory").log());
        }
        if (requestDto.getOrderId() == null || requestDto.getOrderId().isBlank()) {
            return response.status(HttpStatus.BAD_REQUEST)
                    .sendString(Mono.just("Order id is mandatory").log());
        }
        if (requestDto.getAmount() == null || requestDto.getAmount() <= 0d) {
            return response.status(HttpStatus.BAD_REQUEST)
                    .sendString(Mono.just("Amount must be greater than zero").log());
        }
        if (requestDto.getClientId() == null || requestDto.getClientId().getValue() == null) {
            return response.status(HttpStatus.BAD_REQUEST)
                    .sendString(Mono.just("Client id is mandatory").log());
        }

        return paymentRepository.findCardByClient(requestDto.clientId)
                .flatMap(card -> pay(response, card, requestDto.orderId, requestDto.amount))
                .switchIfEmpty(Mono.from(response.status(HttpStatus.NOT_FOUND).
                        sendString(Mono.just(String.format("Client %s was not found or has no associated card", requestDto.clientId.getValue())))));
    }

    private Mono<Void> pay(HttpServerResponse response, Card card, @NonNull String orderId, @NonNull Double amount) {
        // Pay order with id 123, total 342.09 with card number 9999 from client X
        Card.Receipt receipt;
        try {
            receipt = payOrder.using(card, Order.OrderId.of(orderId), Money.of(amount));
        } catch (PaymentError e) {
            e.printStackTrace();
            return Mono.from(response.status(HttpStatus.PAYMENT_REQUIRED)
                    .sendString(Mono.just(e.getMessage()).log()));
        } catch (Exception e) {
            e.printStackTrace();
            return Mono.from(response.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .sendString(Mono.just("Failure processing payment").log()));
        }

        log.info("Order {} was paid using Reactive REST", orderId);

        var status = (receipt instanceof Card.OrderAlreadyPaidReceipt) ? HttpStatus.OK : HttpStatus.CREATED;

        return Mono.from(response.status(status)
                .header("location", "/receipts/" + receipt.getNumber())
                .header("Content-Type", "application/json")
                .sendString(Mono.just(gson.toJson(PaymentReceiptDto.from(receipt)))));
    }

    // REST-related methods
    private Flux<PaymentRequestDto> getPaymentRequestFromPayload(Flux<String> payload) {
        return payload.flatMap(content ->
                Mono.just(gson.fromJson(content, PaymentRequestDto.class))
        );
    }

    @Value
    private static class PaymentRequestDto {
        @NonNull Double amount;
        @NonNull String orderId;
        @NonNull ClientId clientId;
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
