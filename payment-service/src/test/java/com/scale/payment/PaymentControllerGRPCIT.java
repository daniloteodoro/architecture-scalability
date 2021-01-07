package com.scale.payment;

import com.google.protobuf.Timestamp;
import com.scale.payment.application.controller.PaymentGRPCController;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.infrastructure.repository.PaymentRepositoryInMemory;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PaymentControllerGRPCIT {
    private static final int DEFAULT_PORT = 8101;

    @BeforeAll
    static void setup() throws IOException, InterruptedException {
        var paymentRepository = new PaymentRepositoryInMemory();
        var payOrder = new PayOrder(paymentRepository);
        var gRPCController = new PaymentGRPCController(payOrder, paymentRepository);

        PaymentAppUsingGRPC app = new PaymentAppUsingGRPC(gRPCController);
        app.startOnPort(DEFAULT_PORT, false);
    }

    public PaymentServiceGrpc.PaymentServiceBlockingStub createBlockingStub() {
        try {
            var channel = ManagedChannelBuilder.forAddress("127.0.0.1", DEFAULT_PORT)
                    .usePlaintext()
                    .build();
            return PaymentServiceGrpc.newBlockingStub(channel);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failure contacting the Order service using gRPC.");
        }
    }

    @Test
    public void givenAValidCard_WhenPayingANewOrder_ThenReceiptIsGeneratedCorrectly() {
        var stub = createBlockingStub();
        var card = CardDetailsMessage.newBuilder()
                .setNumber("123456")
                .setDigit(555)
                .setExpirationDate("05/2025")
                .build();
        var paymentRequest = PaymentRequestMessage.newBuilder()
                .setCard(card)
                .setAmount(200.0)
                .setOrderId("RANDOM")
                .build();
        var orderReceipt = stub.pay(paymentRequest);
        assertThat(orderReceipt, is(notNullValue()));
        assertThat(orderReceipt.getReceipt(), is(notNullValue()));

        var receipt = orderReceipt.getReceipt();
        assertThat(receipt.getId().length(), is(greaterThan(5)));
        assertThat(receipt.getReference(), is(equalTo("RANDOM")));
        assertThat(receipt.getAmount(), is(closeTo(200.0, 0.00001)));
    }

    private Timestamp toProtoDateTime(ZonedDateTime input) {
        return Timestamp.newBuilder()
                .setSeconds(input.toEpochSecond())
                .setNanos(input.getNano())
                .build();
    }

}
