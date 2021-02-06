package com.scale.payment;

import com.scale.payment.application.controller.PaymentReactiveRESTController;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.domain.repository.PaymentRepository;
import com.scale.payment.infrastructure.configuration.MongoConfig;
import com.scale.payment.infrastructure.repository.PaymentRepositoryInMemory;
import com.scale.payment.infrastructure.repository.PaymentRepositoryMongo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;

@RequiredArgsConstructor
@Slf4j
public class PaymentAppUsingReactiveREST {
    @NonNull PaymentReactiveRESTController paymentRESTController;

    private DisposableServer app = null;

    public static PaymentAppUsingReactiveREST defaultSetup() {
        log.info("Configuring Reactive app using MongoDb");
        var dbConfig = new MongoConfig();

        PaymentRepository paymentRepository = new PaymentRepositoryMongo(dbConfig.getClient(), dbConfig.getDatabase());
        paymentRepository.insertDefaultClientsWithCards();

        var payOrder = new PayOrder(paymentRepository);
        var restController = new PaymentReactiveRESTController(payOrder, paymentRepository);

        return new PaymentAppUsingReactiveREST(restController);
    }

    public static PaymentAppUsingReactiveREST inMemorySetup() {
        log.info("Configuring Reactive app using in-memory persistence");
        PaymentRepository paymentRepository = new PaymentRepositoryInMemory();
        paymentRepository.insertDefaultClientsWithCards();

        var payOrder = new PayOrder(paymentRepository);
        var restController = new PaymentReactiveRESTController(payOrder, paymentRepository);

        return new PaymentAppUsingReactiveREST(restController);
    }

    public void startOnPort(int port) {
        app = HttpServer.create()
                .port(port)
                .route(routes ->
                        routes.get("/hello",
                                (request, response) -> response.sendString(Mono.just("Hello World!")))
//                                        .post("/echo", Application::handleEcho)
//                                                (request, response) -> response.send(request.receive().retain()))
                                .get("/path/{param}",
                                        (request, response) -> response.sendString(Mono.just(request.param("param")))))
                .bindNow();

        app.onDispose()
                .block();

        log.info("Reactive REST Server started, listening on port {}", port);

//        public RouterFunction<ServerResponse> route(PaymentReactiveRESTController paymentController) {
//            return RouterFunctions
//                    .route(POST("/payments").and(accept(MediaType.APPLICATION_JSON)), paymentController::mono);
//
//        }
    }

    public void stop() {
        app.disposeNow(Duration.ofSeconds(10));
    }

}
