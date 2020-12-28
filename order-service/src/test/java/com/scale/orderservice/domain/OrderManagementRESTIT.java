package com.scale.orderservice.domain;

import com.scale.order.OrderAppUsingREST;
import com.scale.order.application.OrderManagementRESTController;
import com.scale.order.domain.model.GenerateOrder;
import com.scale.order.infrastructure.repository.OrderRepositoryInMemory;
import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class OrderManagementRESTIT {
    private static final int DEFAULT_PORT = 8000;
    private static OrderAppUsingREST app = null;

    @BeforeAll
    static void setup() {
        RestAssured.port = DEFAULT_PORT;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        var orderRepository = new OrderRepositoryInMemory();
        var generateOrder = new GenerateOrder(orderRepository);
        var restController = new OrderManagementRESTController(generateOrder, orderRepository);

        app = new OrderAppUsingREST(restController);
        app.startOnPort(DEFAULT_PORT);
    }

    @AfterAll
    static void wrapUp() {
        app.stop();
    }

    @Test
    public void whenPosting3Samples_ThenSessionIsStartedWithCorrectStatusCode() {
        given()
                .body(readFileContent("payloads/create_order.json"))
        .when()
                .post("/orders/convert")
        .then()
                .log().body()
                .statusCode(HttpStatus.SC_CREATED)
                .body(notNullValue())
                .body("id.value", is(notNullValue()))
                .body("items", hasSize(2))
                .body("items[0].quantity", is(equalTo(2)));
    }

    private String readFileContent(String relativePath) {
        var url = OrderManagementRESTIT.class.getClassLoader().getResource(relativePath);
        if (url == null)
            return "";
        try (InputStream is = url.openStream()) {
            BufferedReader in = new BufferedReader(new InputStreamReader(is));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line)
                        .append(System.lineSeparator());
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

}
