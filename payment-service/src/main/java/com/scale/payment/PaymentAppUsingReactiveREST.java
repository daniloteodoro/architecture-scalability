package com.scale.payment;

import com.scale.payment.application.controller.PaymentReactiveRESTController;
import com.scale.payment.application.usecases.PayOrderReactive;
import com.scale.payment.domain.repository.PaymentReactiveRepository;
import com.scale.payment.infrastructure.configuration.MongoConfig;
import com.scale.payment.infrastructure.repository.PaymentReactiveRepositoryInMemory;
import com.scale.payment.infrastructure.repository.PaymentReactiveRepositoryMongo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;

@RequiredArgsConstructor
@Slf4j
public class PaymentAppUsingReactiveREST {
    @NonNull PaymentReactiveRESTController paymentReactiveRESTController;

    private DisposableServer app = null;
    private final long startTime = System.currentTimeMillis();

    public static PaymentAppUsingReactiveREST defaultSetup() {
        log.info("Configuring Reactive app using MongoDb");

        var dbClient = new MongoConfig().getNonBlockingClient();
        PaymentReactiveRepository paymentRepository = new PaymentReactiveRepositoryMongo(dbClient, dbClient.getDatabase("payment_db"));
        paymentRepository.insertDefaultClientsWithCards();

        var payOrder = new PayOrderReactive(paymentRepository);

        // TODO: Remove repository from the controller
        var restController = new PaymentReactiveRESTController(payOrder, paymentRepository);

        return new PaymentAppUsingReactiveREST(restController);
    }

    public static PaymentAppUsingReactiveREST inMemorySetup() {
        log.info("Configuring Reactive app using in-memory persistence");

        var reactiveRepo = new PaymentReactiveRepositoryInMemory();
        reactiveRepo.insertDefaultClientsWithCards();

        var payOrder = new PayOrderReactive(reactiveRepo);

        var restController = new PaymentReactiveRESTController(payOrder, reactiveRepo);

        return new PaymentAppUsingReactiveREST(restController);
    }

    public void startOnPort(int port) {
        app = HttpServer.create()
                .port(port)
                .route(routes ->
                        routes.post("/payments", paymentReactiveRESTController::handlePayment))
                .bindNow();

        log.info("Reactive REST Server started in {}ms, listening on port {}", (System.currentTimeMillis() - startTime), port);

        app.onDispose()
                .block();
    }

    public void stop() {
        app.disposeNow(Duration.ofSeconds(10));
    }

}
