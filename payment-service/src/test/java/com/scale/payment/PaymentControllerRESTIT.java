package com.scale.payment;

import com.scale.payment.application.controller.PaymentRESTController;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.repository.PaymentRepository;
import com.scale.payment.infrastructure.repository.PaymentRepositoryInMemory;
import io.restassured.RestAssured;
import kong.unirest.Unirest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class PaymentControllerRESTIT {
    private static final int DEFAULT_PORT = 8001;
    private static PaymentAppUsingREST app = null;
    private static final String MAKE_PAYMENT_TEMPLATE =
            "{\n" +
            "  \"amount\":200.0,\n" +
            "  \"orderId\":\"%s\",\n" +
            "  \"clientId\": {\n" +
            "      \"value\": \"5ff867a5e77e950006a814ad\"\n" +
            "  }\n" +
            "}";

    @BeforeAll
    static void setup() {
        RestAssured.port = DEFAULT_PORT;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        PaymentRepository paymentRepository = new PaymentRepositoryInMemory();
        paymentRepository.insertDefaultClientsWithCards();

        var RESTController = new PaymentRESTController(new PayOrder(paymentRepository));

        app = new PaymentAppUsingREST(RESTController);
        app.startOnPort(DEFAULT_PORT);
    }

    @AfterAll
    static void wrapUp() {
        app.stop();
    }

    // 16.650  in 10s (single threaded) /   56.434  in 10s (100 threads)
    // 118.008 in 30s                   /   315.720 in 30s (100 threads)
    // 267.568 in 60s    (4.4k/s)       /   710.197 in 60s (100 threads)
    @Test
    public void loadTest() throws InterruptedException {
        int WAIT_TIME_IN_SECONDS = 1;
        long start = System.currentTimeMillis();
        AtomicLong counter = new AtomicLong(0);
        Runnable generatePayments = () -> {
            while (System.currentTimeMillis() - start <= WAIT_TIME_IN_SECONDS * 1000) {
                Unirest.post(String.format("http://127.0.0.1:%d/payments", DEFAULT_PORT))
                    .body(String.format(MAKE_PAYMENT_TEMPLATE, RandomStringUtils.random(10, true, false)))
                    .asEmpty();
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
