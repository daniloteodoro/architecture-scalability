package com.scale.payment;

import com.scale.payment.application.controller.PaymentGRPCController;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentRepository;
import com.scale.payment.infrastructure.repository.PaymentRepositoryInMemory;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PaymentControllerGRPCIT {
    private static final int DEFAULT_PORT = 8101;
    private static final PaymentRepository paymentRepository = new PaymentRepositoryInMemory();

    @BeforeAll
    static void setup() throws IOException, InterruptedException {
        paymentRepository.insertDefaultClientsWithCards();

        var gRPCController = new PaymentGRPCController(new PayOrder(paymentRepository));

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
        storedCard = paymentRepository.findCardByClient(new ClientId("5ff867a5e77e950006a814ad"))
                .orElseThrow(() -> new AssertionError("Card from client 5ff867a5e77e950006a814ad should exist"));
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
                .orElseThrow(() -> new AssertionError("Card from client 5ff878f5e77e950006a814b3 should exist"));
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

        storedCard = paymentRepository.findCardByClient(new ClientId("5ff878f5e77e950006a814b3"))
                .orElseThrow(() -> new AssertionError("Card from client 5ff878f5e77e950006a814b3 should exist"));
        assertThat(storedCard.getLimit(), is(equalTo(Money.of(previousLimit - 200.0))));
    }

    @Test
    public void givenAValidCardWithLowBalance_WhenPayingANewOrder_ThenPaymentIsNotPerformed() {
        var stub = createBlockingStub();
        var storedCard = paymentRepository.findCardByClient(new ClientId("5ff87909e77e950006a814b5"))
                .orElseThrow(() -> new AssertionError("Card from client 5ff87909e77e950006a814b5 should exist"));
        var paymentRequest = PaymentRequestMessage.newBuilder()
                .setClientId("5ff87909e77e950006a814b5")
                .setAmount(storedCard.getLimit().getValue().doubleValue() + 1.0)
                .setOrderId("RANDOM3")
                .build();
        Assertions.assertThrows(StatusRuntimeException.class, () -> stub.pay(paymentRequest));
    }

    //  57.290 in 10s (single threaded) /     256.771 in 10s (100 threads)
    // 241.073 in 30s                   /   1.050.777 in 30s (100 threads)
    // 522.991 in 60s    (8.7k/s)
    @Test
    public void loadTest() throws InterruptedException {
        int WAIT_TIME_IN_SECONDS = 30;
        var stub = createBlockingStub();
        long start = System.currentTimeMillis();
        AtomicLong counter = new AtomicLong(0);
        Runnable generatePayments = () -> {
            while (System.currentTimeMillis() - start <= WAIT_TIME_IN_SECONDS * 1000) {
                var paymentRequest = PaymentRequestMessage.newBuilder()
                        .setClientId("5ff867a5e77e950006a814ad")
                        .setAmount(200.0)
                        .setOrderId(RandomStringUtils.random(10, true, false))
                        .build();
                stub.pay(paymentRequest);
                counter.incrementAndGet();
            }
            System.out.println(Thread.currentThread().getName() + " done");
        };
        IntStream.rangeClosed(1, 100)
                .mapToObj((nr) -> createNamedThread("PaymentWorker-"+nr, generatePayments))
                .forEach(Thread::start);
        TimeUnit.SECONDS.sleep(WAIT_TIME_IN_SECONDS);
        System.out.println(counter.get());
    }

    private Thread createNamedThread(String name, Runnable runnable) {
        var result = new Thread(runnable);
        result.setName(name);
        return result;
    }

}
