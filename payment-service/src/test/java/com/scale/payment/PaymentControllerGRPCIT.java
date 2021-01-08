package com.scale.payment;

import com.google.protobuf.Timestamp;
import com.scale.payment.application.controller.PaymentGRPCController;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentRepository;
import com.scale.payment.infrastructure.repository.PaymentRepositoryInMemory;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PaymentControllerGRPCIT {
    private static final int DEFAULT_PORT = 8101;
    private static final PaymentRepository paymentRepository = new PaymentRepositoryInMemory();

    @BeforeAll
    static void setup() throws IOException, InterruptedException {
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
        paymentRepository.addCard(new Card("123456", (short)555, new Card.ExpirationDate("05/2025"), Money.of(200.0)));
        var stub = createBlockingStub();
        var card = CardDetailsMessage.newBuilder()
                .setNumber("123456")
                .setDigit(555)
                .setExpirationDate("05/2025")
                .build();
        var paymentRequest = PaymentRequestMessage.newBuilder()
                .setCard(card)
                .setAmount(200.0)
                .setOrderId("RANDOM1")
                .build();
        var orderReceipt = stub.pay(paymentRequest);
        assertThat(orderReceipt, is(notNullValue()));
        assertThat(orderReceipt.getReceipt(), is(notNullValue()));

        var receipt = orderReceipt.getReceipt();
        assertThat(receipt.getId().length(), is(greaterThan(5)));
        assertThat(receipt.getReference(), is(equalTo("RANDOM1")));
        assertThat(receipt.getAmount(), is(closeTo(200.0, 0.00001)));

        var storedCard = paymentRepository.findCard("123456", (short)555, new Card.ExpirationDate("05/2025"))
                .orElseThrow(() -> new AssertionError("Card 123456 should exist"));
        assertThat(storedCard.getLimit(), is(equalTo(Money.of(0.0))));
    }

    @Test
    public void givenAValidCard_WhenPayingAnOrderTwice_ThenSecondPaymentIsNotPerformed() {
        paymentRepository.addCard(new Card("333444", (short)555, new Card.ExpirationDate("05/2025"), Money.of(400.0)));
        var stub = createBlockingStub();
        var card = CardDetailsMessage.newBuilder()
                .setNumber("333444")
                .setDigit(555)
                .setExpirationDate("05/2025")
                .build();
        var paymentRequest = PaymentRequestMessage.newBuilder()
                .setCard(card)
                .setAmount(200.0)
                .setOrderId("RANDOM2")
                .build();

        // Pay this order 1st time
        stub.pay(paymentRequest);
        // Pay second time
        var orderReceipt = stub.pay(paymentRequest);

        assertThat(orderReceipt, is(notNullValue()));
        assertThat(orderReceipt.getReceiptOrWarningCase(), is(OrderPaymentDetailMessage.ReceiptOrWarningCase.ORDERALREADYPAID));

        var receipt = orderReceipt.getOrderAlreadyPaid();
        assertThat(receipt.getId().length(), is(greaterThan(5)));
        assertThat(receipt.getReference(), is(equalTo("RANDOM2")));
        assertThat(receipt.getAmount(), is(closeTo(200.0, 0.00001)));

        var storedCard = paymentRepository.findCard("333444", (short)555, new Card.ExpirationDate("05/2025"))
                .orElseThrow(() -> new AssertionError("Card 333444 should exist"));
        assertThat(storedCard.getLimit(), is(equalTo(Money.of(200.0))));
    }

    @Test
    public void givenAValidCardWithLowBalance_WhenPayingANewOrder_ThenPaymentIsNotPerformed() {
        paymentRepository.addCard(new Card("777777", (short)555, new Card.ExpirationDate("05/2025"), Money.of(199.99)));
        var stub = createBlockingStub();
        var card = CardDetailsMessage.newBuilder()
                .setNumber("777777")
                .setDigit(555)
                .setExpirationDate("05/2025")
                .build();
        var paymentRequest = PaymentRequestMessage.newBuilder()
                .setCard(card)
                .setAmount(200.0)
                .setOrderId("RANDOM3")
                .build();
        Assertions.assertThrows(StatusRuntimeException.class, () -> stub.pay(paymentRequest));
    }

}
