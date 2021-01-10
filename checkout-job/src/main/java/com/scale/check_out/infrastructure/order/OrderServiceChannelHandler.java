package com.scale.check_out.infrastructure.order;

import com.scale.check_out.application.services.CheckOutError;
import com.scale.order.OrderServiceGrpc;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderServiceChannelHandler {

    public OrderServiceGrpc.OrderServiceBlockingStub createBlockingStub() {
        try {
            String orderApiHost = System.getenv().getOrDefault("ORDER_API_HOST", "127.0.0.1");
            log.info("Using order-api host: {}", orderApiHost);

            var channel = ManagedChannelBuilder.forAddress(orderApiHost, 8000)
                    .usePlaintext()
                    .build();
            return OrderServiceGrpc.newBlockingStub(channel);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CheckOutError("Failure contacting the Order service using gRPC.");
        }
    }

}
