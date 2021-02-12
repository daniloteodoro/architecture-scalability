package com.scale.check_out;

import com.scale.check_out.application.controller.MetricsController;
import com.scale.check_out.application.controller.ShoppingCartListener;
import com.scale.check_out.application.usecases.PlaceOrder;
import com.scale.check_out.application.usecases.PlaceOrderReactive;
import com.scale.check_out.infrastructure.configuration.SerializerConfig;
import com.scale.check_out.infrastructure.metrics.BusinessMetricsInMemory;
import com.scale.check_out.infrastructure.order.OrderServiceChannelHandler;
import com.scale.check_out.infrastructure.order.OrderServiceUsingGRPC;
import com.scale.check_out.infrastructure.order.OrderServiceUsingREST;
import com.scale.check_out.infrastructure.order.OrderServiceUsingReactiveREST;
import com.scale.check_out.infrastructure.payment.PaymentServiceChannelHandler;
import com.scale.check_out.infrastructure.payment.PaymentServiceUsingREST;
import com.scale.check_out.infrastructure.payment.PaymentServiceUsingGRPC;
import com.scale.check_out.infrastructure.payment.PaymentServiceUsingReactiveREST;
import com.scale.check_out.infrastructure.queue.RabbitMQChannelHandler;
import io.javalin.Javalin;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CheckOutApp {
    @NonNull MetricsController metricsController;
    @NonNull ShoppingCartListener shoppingCartListener;

    private Javalin app = null;

    public void startOnPort(int port) {
        app = Javalin.create().start(port);
        app.get("/metrics", metricsController::handleMetrics);

        shoppingCartListener.start();

        log.info("Check-out job started successfully");
    }

    public void stop() {
        shoppingCartListener.stop();
        app.stop();
    }

    public static CheckOutApp setupGRPC() {
        log.info("Setting up Check-out job using gRPC");

        var inMemoryMetricsWatcher = new BusinessMetricsInMemory();
        var metricsController = new MetricsController(inMemoryMetricsWatcher);
        var orderServiceChannelHandler = new OrderServiceChannelHandler();
        var orderServiceGRPC = new OrderServiceUsingGRPC(orderServiceChannelHandler.createBlockingStub());
        var paymentServiceChannelHandler = new PaymentServiceChannelHandler();
        var paymentServiceGRPC = new PaymentServiceUsingGRPC(paymentServiceChannelHandler.createBlockingStub());
        var placeOrder = new PlaceOrder(orderServiceGRPC, paymentServiceGRPC, orderServiceGRPC, inMemoryMetricsWatcher);
        var queueManager = new RabbitMQChannelHandler();
        var shoppingCartListener = new ShoppingCartListener(queueManager.createChannel(), inMemoryMetricsWatcher, placeOrder);

        return new CheckOutApp(metricsController, shoppingCartListener);
    }

    public static CheckOutApp setupREST() {
        log.info("Setting up Check-out job using REST");

        var inMemoryMetricsWatcher = new BusinessMetricsInMemory();
        var metricsController = new MetricsController(inMemoryMetricsWatcher);

        SerializerConfig.initializeUniRestWithGson();
        String orderApiHost = System.getenv().getOrDefault("ORDER_API_HOST", "127.0.0.1");
        String paymentApiHost = System.getenv().getOrDefault("PAYMENT_API_HOST", "127.0.0.1");
        log.info("Using order-api host: {}", orderApiHost);
        log.info("Using payment-api host: {}", paymentApiHost);

        var orderServiceREST = new OrderServiceUsingREST(orderApiHost, 8000);
        var paymentServiceREST = new PaymentServiceUsingREST(paymentApiHost, 8100);
        var placeOrder = new PlaceOrder(orderServiceREST, paymentServiceREST, orderServiceREST, inMemoryMetricsWatcher);

        var queueManager = new RabbitMQChannelHandler();
        var shoppingCartListener = new ShoppingCartListener(queueManager.createChannel(), inMemoryMetricsWatcher, placeOrder);

        return new CheckOutApp(metricsController, shoppingCartListener);
    }

    public static CheckOutApp setupReactiveREST() {
        log.info("Setting up Check-out job using Reactive REST");

        var inMemoryMetricsWatcher = new BusinessMetricsInMemory();
        var metricsController = new MetricsController(inMemoryMetricsWatcher);

//        SerializerConfig.initializeUniRestWithGson();
        String orderApiHost = System.getenv().getOrDefault("ORDER_API_HOST", "127.0.0.1");
        String paymentApiHost = System.getenv().getOrDefault("PAYMENT_API_HOST", "127.0.0.1");
        log.info("Using order-api host: {}", orderApiHost);
        log.info("Using payment-api host: {}", paymentApiHost);

        var orderServiceReactiveREST = new OrderServiceUsingReactiveREST(orderApiHost, 8000);
        var paymentServiceReactiveREST = new PaymentServiceUsingReactiveREST(paymentApiHost, 8100);
        var placeOrder = new PlaceOrderReactive(orderServiceReactiveREST, paymentServiceReactiveREST, orderServiceReactiveREST, inMemoryMetricsWatcher);

        var queueManager = new RabbitMQChannelHandler();

        https://projectreactor.io/docs/rabbitmq/snapshot/reference/  3. Getting Started

        var shoppingCartListener = new ShoppingCartListener(queueManager.createChannel(), inMemoryMetricsWatcher, placeOrder);

        return new CheckOutApp(metricsController, shoppingCartListener);
    }

}
