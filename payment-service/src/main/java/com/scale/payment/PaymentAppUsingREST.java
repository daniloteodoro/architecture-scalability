package com.scale.payment;

import com.google.gson.Gson;
import com.scale.payment.application.controller.PaymentRESTController;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.infrastructure.configuration.MongoConfig;
import com.scale.payment.infrastructure.configuration.SerializerConfig;
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

        var dbConfig = new MongoConfig();

        var paymentRepository = new PaymentRepositoryMongo(dbConfig.getClient(), dbConfig.getDatabase());
        paymentRepository.insertDefaultClientsWithCards();

        var payOrder = new PayOrder(paymentRepository);
        var RESTController = new PaymentRESTController(payOrder, paymentRepository);

        return new PaymentAppUsingREST(RESTController);
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
