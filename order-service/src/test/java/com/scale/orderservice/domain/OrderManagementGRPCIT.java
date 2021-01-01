package com.scale.orderservice.domain;

import com.google.protobuf.Timestamp;
import com.scale.order.*;
import com.scale.order.application.controller.OrderManagementGRPCController;
import com.scale.order.application.usecase.GenerateOrder;
import com.scale.order.application.usecase.UpdateOrder;
import com.scale.order.infrastructure.repository.OrderRepositoryInMemory;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class OrderManagementGRPCIT {
    private static final int DEFAULT_PORT = 8000;

    @BeforeAll
    static void setup() throws IOException, InterruptedException {
        var orderRepository = new OrderRepositoryInMemory();
        var generateOrder = new GenerateOrder(orderRepository);
        var updateOrder = new UpdateOrder(orderRepository);
        var gRPCController = new OrderManagementGRPCController(generateOrder, updateOrder, orderRepository);

        OrderAppUsingGRPC app = new OrderAppUsingGRPC(gRPCController);
        app.startOnPort(DEFAULT_PORT, false);
    }

    public OrderServiceGrpc.OrderServiceBlockingStub createBlockingStub() {
        try {
            var channel = ManagedChannelBuilder.forAddress("127.0.0.1", DEFAULT_PORT)
                    .usePlaintext()
                    .build();
            return OrderServiceGrpc.newBlockingStub(channel);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failure contacting the Order service using gRPC.");
        }
    }

    @Test
    public void whenPosting3Samples_ThenSessionIsStartedWithCorrectStatusCode() {
        var stub = createBlockingStub();
        var sample = ShoppingCartDto.newBuilder()
                .setSessionId("1")
                .setCartDateTime(toProtoDateTime(ZonedDateTime.now(ZoneOffset.UTC)))
                .setClientId("1")
                .setNumberOfCustomers(1L)
                .setIsFirst(true)
                .setIsLast(true)
                .addAllItems(Collections.singletonList(OrderItemDto.newBuilder()
                        .setId("2")
                        .setProduct(ProductDto.newBuilder()
                                .setId(3L)
                                .setName("Any")
                                .setPrice(19.2F)
                                .setStock(12)
                                .build())
                        .setQuantity(4)
                        .build()))
                .build();
        var order = stub.createOrder(sample);
        assertThat(order, is(notNullValue()));
        assertThat(order.getId(), is(notNullValue()));
        assertThat(order.getItemsList().size(), is(greaterThan(0)));
        var firstItem = order.getItemsList().get(0);
        assertThat(firstItem.getQuantity(), is(equalTo(4)));
    }

    private Timestamp toProtoDateTime(ZonedDateTime input) {
        return Timestamp.newBuilder()
                .setSeconds(input.toEpochSecond())
                .setNanos(input.getNano())
                .build();
    }

}
