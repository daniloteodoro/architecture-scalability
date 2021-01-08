package com.scale.order.application.controller;

import com.google.gson.Gson;
import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;
import com.scale.order.application.usecases.ConfirmOrder;
import com.scale.order.application.usecases.GenerateOrder;
import com.scale.order.application.usecases.UpdateOrder;
import com.scale.order.domain.repository.OrderRepository;
import com.scale.order.infrastructure.configuration.SerializerConfig;
import io.javalin.http.Context;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;

@RequiredArgsConstructor
@Slf4j
public class OrderManagementRESTController {
    @NonNull GenerateOrder generateOrder;
    @NonNull UpdateOrder updateOrder;
    @NonNull ConfirmOrder confirmOrder;
    @NonNull OrderRepository orderRepository;

    private final Gson gson = SerializerConfig.buildSerializer();

    public void handleCreateOrder(Context context) {
        String payload = context.body();

        var order = generateOrder.fromShoppingCart(getShoppingCartFromDto(payload));

        log.info("Order {} was generated using REST", order.getId());

        context.header("location", "/orders/" + order.getId().value())
                .json(order)
                .status(HttpStatus.CREATED_201);
    }

    public void handleUpdateOrderAddress(Context context) {
        String orderId = context.pathParam("id", String.class).getOrNull();
        if (orderId == null || orderId.isBlank()) {
            context.status(HttpStatus.BAD_REQUEST_400)
                    .result("Order id is mandatory");
            return;
        }
        String newAddress = context.body();
        if (newAddress.isBlank()) {
            context.status(HttpStatus.BAD_REQUEST_400)
                    .result("Address cannot be blank");
            return;
        }

        updateOrder.changeAddress(Order.OrderId.of(orderId), newAddress);

        log.info("Order address was updated using REST");
        context.status(HttpStatus.NO_CONTENT_204);
    }

    public void handleConfirm(Context context) {
        String orderId = context.pathParam("id", String.class).getOrNull();
        if (orderId == null || orderId.isBlank()) {
            context.status(HttpStatus.BAD_REQUEST_400)
                    .result("Order id is mandatory");
            return;
        }

        String receiptNumber = context.body();
        if (receiptNumber.isBlank()) {
            context.status(HttpStatus.BAD_REQUEST_400)
                    .result("Receipt number is mandatory");
            return;
        }

        confirmOrder.withPaymentReceipt(Order.OrderId.of(orderId), receiptNumber);

        log.info("Order {} was confirmed using REST", orderId);
        context.status(HttpStatus.NO_CONTENT_204);
    }

    // REST-related methods
    private ShoppingCart getShoppingCartFromDto(String payload) {
        return gson.fromJson(payload, ShoppingCart.class);
    }

}
