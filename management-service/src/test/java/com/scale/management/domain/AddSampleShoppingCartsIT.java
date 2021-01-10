package com.scale.management.domain;

import com.scale.management.ManagementApp;
import com.scale.management.application.controller.ShoppingCartController;
import com.scale.management.domain.model.ShoppingCartPublisher;
import com.scale.management.application.usecases.AddSampleShoppingCarts;
import io.restassured.RestAssured;
import org.apache.http.HttpStatus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class AddSampleShoppingCartsIT {
    private static final int DEFAULT_PORT = 9000;
    private static ManagementApp app = null;

    @BeforeAll
    static void setup() {
        RestAssured.port = DEFAULT_PORT;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        var addSampleShoppingCarts = new AddSampleShoppingCarts(mock(ShoppingCartPublisher.class));
        var shoppingCartController = new ShoppingCartController(addSampleShoppingCarts);
        app = new ManagementApp(shoppingCartController);
        app.startOnPort(DEFAULT_PORT);
    }

    @AfterAll
    static void wrapUp() {
        app.stop();
    }

    @Test
    public void whenPosting3Samples_ThenSessionIsStartedWithCorrectStatusCode() {
        given()
                .pathParam("number", 3)
        .when()
                .post("/shopping-cart/samples/{number}")
        .then()
                .log().body()
                .statusCode(HttpStatus.SC_CREATED)
                .body(notNullValue());
    }

    @Test
    public void whenPosting3SamplesFor3Seconds_ThenSessionIsStartedWithCorrectStatusCode() {
        given()
                .pathParam("number", 3)
                .pathParam("period", 3)
        .when()
                .post("/shopping-cart/samples/{number}/for/{period}/seconds")
        .then()
                .log().body()
                .statusCode(HttpStatus.SC_CREATED)
                .body(notNullValue());
    }

}
