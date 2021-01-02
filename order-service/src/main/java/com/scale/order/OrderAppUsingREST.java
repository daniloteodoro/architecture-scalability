package com.scale.order;

import com.google.gson.Gson;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.scale.order.application.controller.OrderManagementRESTController;
import com.scale.order.application.usecases.ConfirmOrder;
import com.scale.order.application.usecases.GenerateOrder;
import com.scale.order.application.usecases.UpdateOrder;
import com.scale.order.infrastructure.config.ZonedDateTimeCodec;
import com.scale.order.infrastructure.configuration.SerializerConfig;
import com.scale.order.infrastructure.repository.OrderRepositoryMongo;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJson;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.*;

@RequiredArgsConstructor
@Slf4j
public class OrderAppUsingREST {
    @NonNull OrderManagementRESTController orderManagementRESTController;

    private Javalin app = null;

    public static OrderAppUsingREST defaultSetup() {

        // TODO: Move to a dedicated Mongo setup class
        ConnectionString connectionString = new ConnectionString(System.getenv().getOrDefault("mongodb.uri", "mongodb://orderservice:s89fsj&2#@127.0.0.1/admin"));
        CodecRegistry pojoCodecRegistry = fromProviders(
                PojoCodecProvider.builder()
                    .automatic(true)
                    .build()
        );
        CodecRegistry codecRegistry = fromRegistries(
                fromCodecs(new ZonedDateTimeCodec()),
                MongoClientSettings.getDefaultCodecRegistry(),
                pojoCodecRegistry);

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(codecRegistry)
                .build();

        MongoClient mongoClient = MongoClients.create(clientSettings);
        MongoDatabase db = mongoClient.getDatabase("order_db");

        var orderRepository = new OrderRepositoryMongo(db);
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
