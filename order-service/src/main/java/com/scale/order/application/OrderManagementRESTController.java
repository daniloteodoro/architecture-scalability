package com.scale.order.application;

import com.google.gson.Gson;
import com.scale.domain.ShoppingCart;
import com.scale.order.domain.model.GenerateOrder;
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
        log.info("Order address was updated using REST");

        context.status(HttpStatus.NO_CONTENT_204);
    }

    public void handleConfirm(Context context) {
        log.info("Order {} was confirmed using REST", 12345);

        context.status(HttpStatus.NO_CONTENT_204);
    }

    // REST-related methods
    private ShoppingCart getShoppingCartFromDto(String payload) {
        return gson.fromJson(payload, ShoppingCart.class);
    }

}
