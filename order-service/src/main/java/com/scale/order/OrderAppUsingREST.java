package com.scale.order;

import com.google.gson.Gson;
import com.scale.order.application.OrderManagementRESTController;
import com.scale.order.domain.model.GenerateOrder;
import com.scale.order.infrastructure.configuration.SerializerConfig;
import com.scale.order.infrastructure.repository.OrderRepositoryInMemory;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJson;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class OrderAppUsingREST {
    @NonNull OrderManagementRESTController orderManagementRESTController;

    private Javalin app = null;

    public static OrderAppUsingREST defaultSetup() {
        var orderRepository = new OrderRepositoryInMemory();
        var generateOrder = new GenerateOrder(orderRepository);
        var restController = new OrderManagementRESTController(generateOrder, orderRepository);

        return new OrderAppUsingREST(restController);
    }

    public void startOnPort(int port) {
        Gson gson = SerializerConfig.buildSerializer();
        JavalinJson.setToJsonMapper(gson::toJson);
        JavalinJson.setFromJsonMapper(gson::fromJson);

        app = Javalin.create()
                .start(port);

        app.post("/orders/convert", orderManagementRESTController::handleCreateOrder);
        app.put("/orders/:id/address", orderManagementRESTController::handleUpdateOrderAddress);
        app.put("/orders/:id/confirm", orderManagementRESTController::handleConfirm);

        log.info("REST Server started, listening on port {}", port);
    }

    public void stop() {
        app.stop();
    }

}
