package com.scale.payment.application.controller;

import com.google.gson.Gson;
import com.scale.domain.Order;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.model.PaymentError;
import com.scale.payment.infrastructure.configuration.SerializerConfig;
import io.javalin.http.Context;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

@RequiredArgsConstructor
@Slf4j
public class PaymentRESTController {
    @NonNull PayOrder payOrder;

    private final Gson gson = SerializerConfig.buildSerializer();

    public void handlePayment(Context context) {
        var request = getPaymentRequestFromPayload(context.body());

        if (request == null || !request.isValid()) {
            context.status(HttpStatus.BAD_REQUEST_400)
                    .result("Payload containing the full payment request is mandatory");
            return;
        }
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
        if (!request.getClientId().isValid()) {
            context.status(HttpStatus.BAD_REQUEST_400)
                    .result("Client id is mandatory");
            return;
        }

        // Pay order with id 123, total 342.09 with card number 9999 from client X
        Card.Receipt receipt;
        try {
            receipt = payOrder.using(request.clientId, Order.OrderId.of(request.getOrderId()), Money.of(request.getAmount()));
        } catch (ClientId.ClientNotFound e) {
            e.printStackTrace();
            context.status(HttpStatus.NOT_FOUND_404)
                    .result(e.getMessage());
            return;
        } catch (PaymentError e) {
            e.printStackTrace();
            context.status(HttpStatus.PAYMENT_REQUIRED_402)
                    .result(e.getMessage());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            context.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
                    .result("Failure processing payment");
            return;
        }

        log.info("Order {} was paid using REST", request.getOrderId());

        var status = (receipt instanceof Card.OrderAlreadyPaidReceipt) ? HttpStatus.OK_200 : HttpStatus.CREATED_201;

        context.header("location", "/receipts/" + receipt.getNumber())
                .json(PaymentReceiptDto.from(receipt))
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
