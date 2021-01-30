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
//import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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

        var payOrder = new PayOrder(paymentRepository);
        var RESTController = new PaymentRESTController(payOrder, paymentRepository);

        app = new PaymentAppUsingREST(RESTController);
        app.startOnPort(DEFAULT_PORT);
    }

    @AfterAll
    static void wrapUp() {
        app.stop();
    }

    // 16.650 @ 10s
    // 118.008 @ 30s
    // 267.568 @ 60s    (4.4k/s)
    @Test
    public void loadTest() {
        int count = 0;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start <= 10_000) {
            Unirest.post(String.format("http://127.0.0.1:%d/payments", DEFAULT_PORT))
                    .body(String.format(MAKE_PAYMENT_TEMPLATE, RandomStringUtils.random(10, true, false)))
                    .asEmpty();
            count++;
        }
        System.out.println(count);
        assertThat(count, is(greaterThan(0)));
    }

}
