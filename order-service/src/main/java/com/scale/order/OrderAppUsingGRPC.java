package com.scale.order;

import com.scale.order.application.controller.OrderManagementGRPCController;
import com.scale.order.application.usecases.GenerateOrder;
import com.scale.order.application.usecases.UpdateOrder;
import com.scale.order.infrastructure.repository.OrderRepositoryInMemory;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
public class OrderAppUsingGRPC {
    @NonNull OrderManagementGRPCController orderManagementGRPCController;

    private Server server;

    public static OrderAppUsingGRPC defaultSetup() {
        var orderRepository = new OrderRepositoryInMemory();
        var generateOrder = new GenerateOrder(orderRepository);
        var updateOrder = new UpdateOrder(orderRepository);
        var gRPCController = new OrderManagementGRPCController(generateOrder, updateOrder, orderRepository);

        return new OrderAppUsingGRPC(gRPCController);
    }

    public void startOnPort(Integer port, boolean awaitTermination) throws IOException, InterruptedException {
        this.server = ServerBuilder.forPort(port)
                .addService(orderManagementGRPCController)
                .build();

        server.start();
        log.info("gRPC Server started, listening on port {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            log.info("*** shutting down gRPC server");
            try {
                OrderAppUsingGRPC.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            log.info("*** server shut down");
        }));
        if (awaitTermination)
            server.awaitTermination();
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

}
