package com.scale.payment;

import com.scale.payment.application.controller.PaymentGRPCController;
import com.scale.payment.application.usecases.PayOrder;
import com.scale.payment.infrastructure.repository.PaymentRepositoryMongo;
import com.scale.payment.infrastructure.configuration.MongoConfig;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
public class PaymentAppUsingGRPC {
    @NonNull PaymentGRPCController paymentGRPCController;

    private Server server;

    public static PaymentAppUsingGRPC defaultSetup() {
        log.info("Configuring app using MongoDb");

        var dbClient = new MongoConfig().getBlockingClient();
        var paymentRepository = new PaymentRepositoryMongo(dbClient, dbClient.getDatabase("payment_db"));
        paymentRepository.insertDefaultClientsWithCards();

        var gRPCController = new PaymentGRPCController(new PayOrder(paymentRepository));

        return new PaymentAppUsingGRPC(gRPCController);
    }

    public void startOnPort(Integer port, boolean awaitTermination) throws IOException, InterruptedException {
        this.server = ServerBuilder.forPort(port)
                .addService(paymentGRPCController)
                .build();

        server.start();
        log.info("gRPC Server started, listening on port {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            log.info("*** shutting down gRPC server");
            try {
                PaymentAppUsingGRPC.this.stop();
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
