package com.scale.check_out;

import com.scale.check_out.application.controller.MetricsController;
import com.scale.check_out.application.controller.ReactiveQueueConsumer;
import com.scale.check_out.application.controller.ShoppingCartReactiveListener;
import com.scale.check_out.application.usecases.PlaceOrderReactive;
import com.scale.check_out.infrastructure.metrics.BusinessMetricsInMemory;
import com.scale.check_out.infrastructure.order.OrderServiceUsingReactiveREST;
import com.scale.check_out.infrastructure.payment.PaymentServiceUsingReactiveREST;
import com.scale.check_out.infrastructure.queue.RabbitMQChannelHandler;
import io.javalin.Javalin;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ReactiveCheckOutApp {
    @NonNull MetricsController metricsController;
    @NonNull ReactiveQueueConsumer queueConsumer;

    private Javalin app = null;
    private static final String queueConnectionUrl =
            System.getenv().getOrDefault("AMQP_URL", "amqp://guest:guest@localhost");

    public void startOnPort(int port) {
        app = Javalin.create().start(port);
        app.get("/metrics", metricsController::handleMetrics);

        queueConsumer.start()
                .subscribe();

        log.info("Reactive Check-out job started successfully");
    }

    public void stop() {
        queueConsumer.stop();
        app.stop();
    }

    public static ReactiveCheckOutApp setupReactiveREST() {
        log.info("Setting up Check-out job using Reactive REST");

        var inMemoryMetricsWatcher = new BusinessMetricsInMemory();
        var metricsController = new MetricsController(inMemoryMetricsWatcher);

        String orderApiHost = System.getenv().getOrDefault("ORDER_API_HOST", "127.0.0.1");
        String paymentApiHost = System.getenv().getOrDefault("PAYMENT_API_HOST", "127.0.0.1");
        log.info("Using order-api host: {}", orderApiHost);
        log.info("Using payment-api host: {}", paymentApiHost);

        var orderServiceReactiveREST = new OrderServiceUsingReactiveREST(orderApiHost, 8000);
        var paymentServiceReactiveREST = new PaymentServiceUsingReactiveREST(paymentApiHost, 8100);
        var placeOrder = new PlaceOrderReactive(orderServiceReactiveREST, paymentServiceReactiveREST, orderServiceReactiveREST, inMemoryMetricsWatcher);

        var queueManager = new RabbitMQChannelHandler(queueConnectionUrl);
        var shoppingCartListener = new ShoppingCartReactiveListener(queueManager, inMemoryMetricsWatcher, placeOrder);

        return new ReactiveCheckOutApp(metricsController, shoppingCartListener);
    }

}
