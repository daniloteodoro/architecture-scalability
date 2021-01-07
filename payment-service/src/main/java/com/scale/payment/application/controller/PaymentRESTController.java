package com.scale.payment.application.controller;

import com.google.gson.Gson;
import com.scale.domain.Order;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentRepository;
import com.scale.payment.infrastructure.configuration.SerializerConfig;
import io.javalin.http.Context;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;

@RequiredArgsConstructor
@Slf4j
public class PaymentRESTController {
    @NonNull PayOrder payOrder;
    @NonNull PaymentRepository paymentRepository;

    private final Gson gson = SerializerConfig.buildSerializer();

    public void handlePayment(Context context) {
        var request = getPaymentRequestFromPayload(context.body());

        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            context.status(HttpStatus.BAD_REQUEST_400)
                    .result("Order id is mandatory");
            return;
        }
        if (request.getAmount() <= 0d) {
            context.status(HttpStatus.BAD_REQUEST_400)
                    .result("Amount must be greater than zero");
            return;
        }

        var requestedCard = paymentRepository.findCard(request.card.number, request.card.digit, new Card.ExpirationDate(request.card.expirationDate));
        if (requestedCard.isEmpty()) {
            context.status(HttpStatus.NOT_FOUND_404)
                    .result("Card was not found");
            return;
        }

        // Pay order with id XXX, total 342.09 with card number NNNN
        Card.Receipt receipt = null;
        try {
            receipt = payOrder.using(requestedCard.get(), Order.OrderId.of(request.getOrderId()), Money.of(request.getAmount()));
        } catch (Exception e) {
            e.printStackTrace();
            context.status(HttpStatus.PAYMENT_REQUIRED_402)
                    .result(e.getMessage());
            return;
        }

        log.info("Order {} was paid using REST", request.getOrderId());

        var status = (receipt instanceof Card.OrderAlreadyPayedReceipt) ? HttpStatus.OK_200 : HttpStatus.CREATED_201;

        context.header("location", "/receipts/" + receipt.getNumber())
                .json(receipt)
                .status(status);
    }

    // REST-related methods
    private PaymentRequestDto getPaymentRequestFromPayload(String payload) {
        return gson.fromJson(payload, PaymentRequestDto.class);
    }

    @Value
    private static class PaymentRequestDto {
        @NonNull Double amount;
        @NonNull String orderId;
        @NonNull CardDetailsDto card;
    }

    @Value
    private static class CardDetailsDto {
        @NonNull String number;
        @NonNull Short digit;
        // MM/YYYY
        @NonNull String expirationDate;
    }

}
