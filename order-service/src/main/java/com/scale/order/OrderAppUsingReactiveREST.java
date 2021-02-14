package com.scale.order;

import com.scale.order.application.controller.OrderManagementReactiveRESTController;
import com.scale.order.application.usecases.ConfirmOrderReactive;
import com.scale.order.application.usecases.GenerateOrderReactive;
import com.scale.order.domain.repository.OrderReactiveRepository;
import com.scale.order.infrastructure.configuration.MongoConfig;
import com.scale.order.infrastructure.repository.OrderReactiveRepositoryMongo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;

@RequiredArgsConstructor
@Slf4j
public class OrderAppUsingReactiveREST {
    @NonNull OrderManagementReactiveRESTController orderManagementReactiveRESTController;

    private DisposableServer app = null;
    private final long startTime = System.currentTimeMillis();

    public static OrderAppUsingReactiveREST defaultSetup() {
        log.info("Configuring Reactive app using MongoDb");

        var dbClient = new MongoConfig().getNonBlockingClient();

        OrderReactiveRepository orderReactiveRepository = new OrderReactiveRepositoryMongo(dbClient.getDatabase("order_db"));
        var generateOrder = new GenerateOrderReactive(orderReactiveRepository);
        var confirmOrder = new ConfirmOrderReactive(orderReactiveRepository);
        var reactiveRESTController = new OrderManagementReactiveRESTController(generateOrder, confirmOrder);

        return new OrderAppUsingReactiveREST(reactiveRESTController);
    }

    public void startOnPort(int port) {
        app = HttpServer.create()
                .port(port)
                .route(routes -> routes
                        .post("/orders/convert", orderManagementReactiveRESTController::handleCreateOrder)
                        .put("/orders/{id}/confirm", orderManagementReactiveRESTController::handleConfirm))
                .bindNow();

        log.info("Reactive REST Server started in {}ms, listening on port {}", (System.currentTimeMillis() - startTime), port);

        app.onDispose()
                .block();
    }

    public void stop() {
        app.disposeNow(Duration.ofSeconds(10));
    }

}
