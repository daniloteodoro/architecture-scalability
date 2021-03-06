package com.scale.payment.application.controller;

import com.google.protobuf.Timestamp;
import com.scale.domain.Order;
import com.scale.payment.OrderPaymentDetailMessage;
import com.scale.payment.PaymentReceiptMessage;
import com.scale.payment.PaymentRequestMessage;
import com.scale.payment.PaymentServiceGrpc;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.model.PaymentError;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@RequiredArgsConstructor
@Slf4j
public class PaymentGRPCController extends PaymentServiceGrpc.PaymentServiceImplBase {
    @NonNull PayOrder payOrder;

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
        if (request.getClientId().isBlank()) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("Client id is mandatory")
                    .asRuntimeException());
            return;
        }

        // Pay order with id 123, total 342.09 with card number 9999 from client X
        Card.Receipt receipt;
        try {
            receipt = payOrder.using(new ClientId(request.getClientId()), Order.OrderId.of(request.getOrderId()), Money.of(request.getAmount()));
        } catch (ClientId.ClientNotFound e) {
            e.printStackTrace();
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
            return;
        } catch (PaymentError e) {
            e.printStackTrace();
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failure processing payment")
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
                .setNumber(receipt.getNumber())
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
