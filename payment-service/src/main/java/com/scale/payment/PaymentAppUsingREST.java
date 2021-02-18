package com.scale.payment;

import com.google.gson.Gson;
import com.scale.payment.application.controller.PaymentRESTController;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.repository.PaymentRepository;
import com.scale.payment.infrastructure.configuration.MongoConfig;
import com.scale.payment.infrastructure.configuration.SerializerConfig;
import com.scale.payment.infrastructure.repository.PaymentRepositoryInMemory;
import com.scale.payment.infrastructure.repository.PaymentRepositoryMongo;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJson;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class PaymentAppUsingREST {
    @NonNull PaymentRESTController paymentRESTController;

    private Javalin app = null;

    public static PaymentAppUsingREST defaultSetup() {
        log.info("Configuring app using MongoDb");

        var dbClient = new MongoConfig().getBlockingClient();
        var paymentRepository = new PaymentRepositoryMongo(dbClient, dbClient.getDatabase("payment_db"));
        paymentRepository.insertDefaultClientsWithCards();

        var RESTController = new PaymentRESTController(new PayOrder(paymentRepository));

        return new PaymentAppUsingREST(RESTController);
    }

    public static PaymentAppUsingREST inMemorySetup() {
        log.info("Configuring app using in-memory persistence");
        PaymentRepository paymentRepository = new PaymentRepositoryInMemory();
        paymentRepository.insertDefaultClientsWithCards();

        var restController = new PaymentRESTController(new PayOrder(paymentRepository));

        return new PaymentAppUsingREST(restController);
    }

    public void startOnPort(int port) {
        Gson gson = SerializerConfig.buildSerializer();
        JavalinJson.setToJsonMapper(gson::toJson);
        JavalinJson.setFromJsonMapper(gson::fromJson);

        app = Javalin.create()
                .start(port);

        app.post("/payments", paymentRESTController::handlePayment);

        log.info("REST Server started, listening on port {}", port);
    }

    public void stop() {
        app.stop();
    }

}
