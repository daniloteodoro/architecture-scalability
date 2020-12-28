package com.scale.management;

import com.scale.management.application.ShoppingCartController;
import com.scale.management.domain.usecases.AddSampleShoppingCarts;
import com.scale.management.infrastructure.shoppingcart.ShoppingCartPublisherUsingAMQP;
import com.scale.management.infrastructure.kibana.ConfigureDashboards;
import com.scale.management.infrastructure.queue.RabbitMQChannelHandler;
import io.javalin.Javalin;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class ManagementApp {
    @NonNull ShoppingCartController shoppingCartController;

    private Javalin app = null;

    public void startOnPort(int port) {
        app = Javalin.create().start(port);
        app.post("/shopping-cart/samples/:number", shoppingCartController::handleNewSamples);
        app.post("/shopping-cart/samples/:number/for/:interval/seconds", shoppingCartController::handleNewSamplesEveryXSeconds);
    }

    public void stop() {
        app.stop();
    }

    public static ManagementApp createUsingRESTAndAMQP() throws IOException {
        // Setup using AMQP (RabbitMQ) and REST (Javalin)
        var queueManager = new RabbitMQChannelHandler();
        var publisher = new ShoppingCartPublisherUsingAMQP(queueManager.createChannel());
        var addSampleShoppingCarts = new AddSampleShoppingCarts(publisher);
        var shoppingCartController = new ShoppingCartController(addSampleShoppingCarts);

        return new ManagementApp(shoppingCartController);
    }

    public static void main(String[] args) {
        try {
            String paramPort = args.length > 0 ? args[0] : "9000";

            var app = ManagementApp.createUsingRESTAndAMQP();
            app.startOnPort(Integer.parseInt(paramPort));

            ConfigureDashboards.forKibana();

            log.info("App started listening on port " + paramPort);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}
