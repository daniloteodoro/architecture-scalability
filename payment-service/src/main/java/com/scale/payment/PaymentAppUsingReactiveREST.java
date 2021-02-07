package com.scale.payment;

import com.scale.payment.application.controller.PaymentReactiveRESTController;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.application.usecases.PayOrderReactive;
import com.scale.payment.domain.repository.PaymentRepository;
import com.scale.payment.infrastructure.configuration.MongoConfig;
import com.scale.payment.infrastructure.repository.PaymentReactiveRepositoryInMemory;
import com.scale.payment.infrastructure.repository.PaymentRepositoryInMemory;
import com.scale.payment.infrastructure.repository.PaymentRepositoryMongo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRoutes;

import java.time.Duration;

@RequiredArgsConstructor
@Slf4j
public class PaymentAppUsingReactiveREST {
    @NonNull PaymentReactiveRESTController paymentReactiveRESTController;

    private DisposableServer app = null;

    public static PaymentAppUsingReactiveREST defaultSetup() {
        log.info("Configuring Reactive app using MongoDb (temporarily using in-memory)");
        var dbConfig = new MongoConfig();

        // TODO: Use reactive MONGODB repository
//        PaymentRepository paymentRepository = new PaymentRepositoryMongo(dbConfig.getClient(), dbConfig.getDatabase());
//        paymentRepository.insertDefaultClientsWithCards();

        var reactiveRepo = new PaymentReactiveRepositoryInMemory();
        reactiveRepo.insertDefaultClientsWithCards();

        var payOrder = new PayOrderReactive(reactiveRepo);

        var restController = new PaymentReactiveRESTController(payOrder, reactiveRepo);

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

        log.info("Reactive REST Server started, listening on port {}", port);

        app.onDispose()
                .block();
    }

    public void stop() {
        app.disposeNow(Duration.ofSeconds(10));
    }

}
