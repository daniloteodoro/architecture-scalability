package com.scale.payment.application.controller;

import com.google.protobuf.Timestamp;
import com.scale.domain.Order;
import com.scale.payment.OrderPaymentDetailMessage;
import com.scale.payment.PaymentReceiptMessage;
import com.scale.payment.PaymentRequestMessage;
import com.scale.payment.PaymentServiceGrpc;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.model.PaymentError;
import com.scale.payment.domain.repository.PaymentRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@RequiredArgsConstructor
@Slf4j
public class PaymentGRPCController extends PaymentServiceGrpc.PaymentServiceImplBase {
    @NonNull PayOrder payOrder;
    @NonNull PaymentRepository paymentRepository;

    @Override
    public void pay(PaymentRequestMessage request, StreamObserver<OrderPaymentDetailMessage> responseObserver) {
        if (request.getOrderId().isBlank()) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Order id is mandatory")
                    .asRuntimeException());
            return;
        }
        if (request.getAmount() <= 0d) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Amount must be greater than zero")
                    .asRuntimeException());
            return;
        }

        var requestedCard = paymentRepository.findCard(request.getCard().getNumber(),
                (short)request.getCard().getDigit(),
                new Card.ExpirationDate(request.getCard().getExpirationDate()));
        if (requestedCard.isEmpty()) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Card was not found")
                    .asRuntimeException());
            return;
        }

        // Pay order with id XXX, total 342.09 with card number NNNN
        Card.Receipt receipt = null;
        try {
            receipt = payOrder.using(requestedCard.get(), Order.OrderId.of(request.getOrderId()), Money.of(request.getAmount()));
        } catch (PaymentError e) {
            e.printStackTrace();
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
            return;
        }

        log.info("Order {} was paid using gRPC", request.getOrderId());

        var now = ZonedDateTime.now(ZoneOffset.UTC);
        Timestamp receiptTime = Timestamp.newBuilder()
                .setSeconds(now.toEpochSecond())
                .setNanos(now.getNano())
                .build();

        var paymentReceipt = PaymentReceiptMessage.newBuilder()
                .setId(receipt.getNumber())
                .setReference(receipt.getReference())
                .setTime(receiptTime)
                .setAmount(receipt.getAmount().getValue().doubleValue())
                .build();

        var orderPayment = OrderPaymentDetailMessage.newBuilder();
        if (receipt instanceof Card.OrderAlreadyPaidReceipt)
            orderPayment.setOrderAlreadyPaid(paymentReceipt);
        else
            orderPayment.setReceipt(paymentReceipt);

        responseObserver.onNext(orderPayment.build());
        responseObserver.onCompleted();
    }

}
