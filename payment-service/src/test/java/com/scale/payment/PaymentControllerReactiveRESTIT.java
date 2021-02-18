package com.scale.payment;

import com.scale.payment.application.controller.PaymentReactiveRESTController;
import com.scale.payment.application.usecases.PayOrderReactive;
import com.scale.payment.domain.repository.PaymentReactiveRepository;
import com.scale.payment.infrastructure.repository.PaymentReactiveRepositoryInMemory;
import io.restassured.RestAssured;
import kong.unirest.Unirest;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class PaymentControllerReactiveRESTIT {
    private static PaymentAppUsingReactiveREST app = null;
    private static final int DEFAULT_PORT = 8101;
    private static final String MAKE_PAYMENT_TEMPLATE =
            """
                {
                  "amount":200.0,
                  "orderId":"%s",
                  "clientId": {
                      "value": "5ff867a5e77e950006a814ad"
                  }
                }
            """;
    private static final String PAYMENT_WITH_EXPIRED_CARD_TEMPLATE =
            """
                {
                  "amount":200.0,
                  "orderId":"%s",
                  "clientId": {
                      "value": "client_with_card_expired"
                  }
                }
            """;

    @BeforeAll
    static void setup() {
        RestAssured.port = DEFAULT_PORT;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        PaymentReactiveRepository paymentRepository = new PaymentReactiveRepositoryInMemory();
        paymentRepository.insertDefaultClientsWithCards();

        var RESTController = new PaymentReactiveRESTController(new PayOrderReactive(paymentRepository));

        app = new PaymentAppUsingReactiveREST(RESTController);
        app.startOnPort(DEFAULT_PORT, false);
    }

    @AfterAll
    static void wrapUp() {
        app.stop();
    }

    @Test
    public void givenAnExpiredCard_WhenProcessingPayment_A402PaymentRequiredIsReturned() {
        given()
                .body(String.format(PAYMENT_WITH_EXPIRED_CARD_TEMPLATE, RandomStringUtils.random(10, true, false)))
        .when()
                .post("/payments")
        .then()
                .log().body()
                .statusCode(HttpStatus.SC_PAYMENT_REQUIRED);
    }

    @Test
    public void givenAnEmptyOrder_WhenProcessingPayment_A400BadRequestIsReturned() {
        given()
            .body(String.format(PAYMENT_WITH_EXPIRED_CARD_TEMPLATE, ""))
        .when()
            .post("/payments")
        .then()
            .log().body()
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .body(is("Order id is mandatory"));
    }

    @Test
    public void givenAValidCard_WhenPayingAnOrderTwice_ThenSecondPaymentIsNotPerformed() {
        String paymentRequest = String.format(MAKE_PAYMENT_TEMPLATE, "RANDOM2");
        given()
            .body(paymentRequest)
        .when()
            .post("/payments")
        .then()
            .log().body()
            .statusCode(HttpStatus.SC_CREATED);

        given()
            .body(paymentRequest)
        .when()
            .post("/payments")
        .then()
            .log().body()
            .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void givenAValidCard_WhenPayingANewOrder_ThenReceiptIsGeneratedCorrectly() {
        String orderReference = RandomStringUtils.random(10, true, false);
        given()
            .body(String.format(MAKE_PAYMENT_TEMPLATE, orderReference))
        .when()
            .post("/payments")
        .then()
            .log().body()
            .statusCode(HttpStatus.SC_CREATED)
            .body("number", is(notNullValue()))
            .body("reference", is(equalTo(orderReference)))
            .body("amount", is(200f));
    }

    // 14.902  in 10s (single threaded) /   67.238  in 10s (100 threads)
    // 112.550 in 30s                   /   387.345 in 30s (100 threads)
    // 265.951 in 60s (4.4k/s)          /   841.786 in 60s (100 threads)
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
