package com.scale.payment.application.controller;

import java.time.ZonedDateTime;
import com.google.gson.Gson;
import com.scale.domain.Order;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.model.PaymentError;
import com.scale.payment.domain.repository.PaymentRepository;
import com.scale.payment.infrastructure.configuration.SerializerConfig;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class PaymentReactiveRESTController {
    @NonNull PayOrder payOrder;
    @NonNull PaymentRepository paymentRepository;

    private final Gson gson = SerializerConfig.buildSerializer();

//    public Mono<ServerResponse> mono(ServerRequest serverRequest) {
//        return ServerResponse.ok()
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(
//                        Mono.just(1)
//                        /*.log()*/, Integer.class
//                );
//    }

//    public Mono<ServerResponse> handlePayment(ServerRequest serverRequest) {
//        var request = getPaymentRequestFromPayload(context.body());
//
//        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
//            return ServerResponse.badRequest()
//                    .bodyValue("Order id is mandatory");
//        }
//        if (request.getAmount() <= 0d) {
//            return ServerResponse.badRequest()
//                    .bodyValue("Amount must be greater than zero");
//        }
//        if (request.getClientId() == null) {
//            return ServerResponse.badRequest()
//                    .bodyValue("Client id is mandatory");
//        }
//
//        // TODO: Add non-blocking
//        var requestedCard = paymentRepository.findCardByClient(request.clientId);
//        if (requestedCard.isEmpty()) {
//            return ServerResponse.notFound()
//                    .header("reason", String.format("Client %s was not found or has no associated card", request.clientId.getValue()))
//                    .build();
//        }
//
//        // Pay order with id 123, total 342.09 with card number 9999 from client X
//        Card.Receipt receipt;
//        try {
//            receipt = payOrder.using(requestedCard.get(), Order.OrderId.of(request.getOrderId()), Money.of(request.getAmount()));
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
//        log.info("Order {} was paid using Reactive REST", request.getOrderId());
//
//        var status = (receipt instanceof Card.OrderAlreadyPaidReceipt) ? HttpStatus.OK_200 : HttpStatus.CREATED_201;
//
//        context.header("location", "/receipts/" + receipt.getNumber())
//                .json(PaymentReceiptDto.from(receipt))
//                .status(status);
//        return ServerResponse.status(status)
//                .header("location", "/receipts/" + receipt.getNumber())
//                .body(Mono.just(PaymentReceiptDto.from(receipt)));
//    }

    // REST-related methods
    private PaymentRequestDto getPaymentRequestFromPayload(String payload) {
        return gson.fromJson(payload, PaymentRequestDto.class);
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
