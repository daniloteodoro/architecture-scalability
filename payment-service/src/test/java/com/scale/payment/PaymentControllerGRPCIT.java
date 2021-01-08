package com.scale.payment;

import com.scale.payment.application.controller.PaymentGRPCController;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentRepository;
import com.scale.payment.infrastructure.repository.PaymentRepositoryInMemory;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PaymentControllerGRPCIT {
    private static final int DEFAULT_PORT = 8101;
    private static final PaymentRepository paymentRepository = new PaymentRepositoryInMemory();

    @BeforeAll
    static void setup() throws IOException, InterruptedException {
        paymentRepository.insertDefaultClientsWithCards();

        var payOrder = new PayOrder(paymentRepository);
        var gRPCController = new PaymentGRPCController(payOrder, paymentRepository);

        new PaymentAppUsingGRPC(gRPCController)
                .startOnPort(DEFAULT_PORT, false);
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
        var paymentRequest = PaymentRequestMessage.newBuilder()
                .setClientId("5ff867a5e77e950006a814ad")
                .setAmount(200.0)
                .setOrderId("RANDOM1")
                .build();
        var storedCard = paymentRepository.findCardByClient(new ClientId("5ff867a5e77e950006a814ad"))
                .orElseThrow(() -> new AssertionError("Card from client 5ff867a5e77e950006a814ad should exist"));
        var previousLimit = storedCard.getLimit().getValue().doubleValue();

        var orderReceipt = stub.pay(paymentRequest);
        assertThat(orderReceipt, is(notNullValue()));
        assertThat(orderReceipt.getReceipt(), is(notNullValue()));

        var receipt = orderReceipt.getReceipt();
        assertThat(receipt.getNumber().length(), is(greaterThan(5)));
        assertThat(receipt.getReference(), is(equalTo("RANDOM1")));
        assertThat(receipt.getAmount(), is(closeTo(200.0, 0.00001)));

        // This is the card associated with client id 5ff867a5e77e950006a814ad, inserted on the startup by method PaymentRepository.insertDefaultClientsWithCards()
        storedCard = paymentRepository.findCardByClient(new ClientId("5ff867a5e77e950006a814ad")).get();
        assertThat(storedCard.getLimit(), is(equalTo(Money.of(previousLimit - 200.0))));
    }

    @Test
    public void givenAValidCard_WhenPayingAnOrderTwice_ThenSecondPaymentIsNotPerformed() {
        var stub = createBlockingStub();
        var paymentRequest = PaymentRequestMessage.newBuilder()
                .setClientId("5ff878f5e77e950006a814b3")
                .setAmount(200.0)
                .setOrderId("RANDOM2")
                .build();
        var storedCard = paymentRepository.findCardByClient(new ClientId("5ff878f5e77e950006a814b3"))
                .orElseThrow(() -> new AssertionError("Card 5ff873c1e77e950006a814af should exist"));
        var previousLimit = storedCard.getLimit().getValue().doubleValue();

        // Pay this order 1st time
        stub.pay(paymentRequest);
        // Pay second time
        var orderReceipt = stub.pay(paymentRequest);

        assertThat(orderReceipt, is(notNullValue()));
        assertThat(orderReceipt.getReceiptOrWarningCase(), is(OrderPaymentDetailMessage.ReceiptOrWarningCase.ORDERALREADYPAID));

        var receipt = orderReceipt.getOrderAlreadyPaid();
        assertThat(receipt.getNumber().length(), is(greaterThan(5)));
        assertThat(receipt.getReference(), is(equalTo("RANDOM2")));
        assertThat(receipt.getAmount(), is(closeTo(200.0, 0.00001)));

        storedCard = paymentRepository.findCardByClient(new ClientId("5ff878f5e77e950006a814b3")).get();
        assertThat(storedCard.getLimit(), is(equalTo(Money.of(previousLimit - 200.0))));
    }

    @Test
    public void givenAValidCardWithLowBalance_WhenPayingANewOrder_ThenPaymentIsNotPerformed() {
        var stub = createBlockingStub();
        var storedCard = paymentRepository.findCardByClient(new ClientId("5ff87909e77e950006a814b5"))
                .orElseThrow(() -> new AssertionError("Card 5ff87909e77e950006a814b5 should exist"));
        var paymentRequest = PaymentRequestMessage.newBuilder()
                .setClientId("5ff87909e77e950006a814b5")
                .setAmount(storedCard.getLimit().getValue().doubleValue() + 1.0)
                .setOrderId("RANDOM3")
                .build();
        Assertions.assertThrows(StatusRuntimeException.class, () -> stub.pay(paymentRequest));
    }

}
