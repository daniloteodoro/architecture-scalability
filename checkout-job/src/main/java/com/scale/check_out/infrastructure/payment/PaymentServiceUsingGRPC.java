package com.scale.check_out.infrastructure.payment;

import com.google.protobuf.Timestamp;
import com.scale.check_out.application.services.payment.PayOrder;
import com.scale.check_out.application.services.payment.PaymentDto;
import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;
import com.scale.payment.OrderPaymentDetailMessage;
import com.scale.payment.PaymentRequestMessage;
import com.scale.payment.PaymentServiceGrpc;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@RequiredArgsConstructor
@Slf4j
public class PaymentServiceUsingGRPC implements PayOrder {
    @NonNull PaymentServiceGrpc.PaymentServiceBlockingStub paymentService;

    @Override
    public PaymentDto.PaymentReceiptDto with(ShoppingCart.ClientId clientId, Order order) {
        var paymentRequest = PaymentRequestMessage.newBuilder()
                .setClientId(clientId.value())
                .setOrderId(order.getId().value())
                .setAmount(order.calculateTotal())
                .build();

        var response = paymentService.pay(paymentRequest);
        var responseContent =
                (response.getReceiptOrWarningCase() == OrderPaymentDetailMessage.ReceiptOrWarningCase.RECEIPT) ?
                        response.getReceipt() : response.getOrderAlreadyPaid();

        return new PaymentDto.PaymentReceiptDto(
                responseContent.getNumber(),
                toUTCDateTime(responseContent.getTime()),
                responseContent.getReference(),
                responseContent.getAmount());
    }

    private ZonedDateTime toUTCDateTime(Timestamp input) {
        return Instant
                .ofEpochSecond( input.getSeconds(), input.getNanos() )
                .atOffset( ZoneOffset.UTC )
                .toZonedDateTime();
    }

}
