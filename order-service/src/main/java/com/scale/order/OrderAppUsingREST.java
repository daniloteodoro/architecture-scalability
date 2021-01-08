package com.scale.order;

import com.google.gson.Gson;
import com.scale.order.application.controller.OrderManagementRESTController;
import com.scale.order.application.usecases.ConfirmOrder;
import com.scale.order.application.usecases.GenerateOrder;
import com.scale.order.application.usecases.UpdateOrder;
import com.scale.order.infrastructure.configuration.MongoConfig;
import com.scale.order.infrastructure.configuration.SerializerConfig;
import com.scale.order.infrastructure.repository.OrderRepositoryMongo;
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

        var dbConfig = new MongoConfig();

        var orderRepository = new OrderRepositoryMongo(dbConfig.getDatabase());
        var generateOrder = new GenerateOrder(orderRepository);
        var updateOrder = new UpdateOrder(orderRepository);
        var confirmOrder = new ConfirmOrder(orderRepository);
        var restController = new OrderManagementRESTController(generateOrder, updateOrder, confirmOrder, orderRepository);

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
