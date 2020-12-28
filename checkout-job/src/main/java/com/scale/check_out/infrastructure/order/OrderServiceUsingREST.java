package com.scale.check_out.infrastructure.order;

import com.scale.check_out.domain.model.ConfirmOrder;
import com.scale.check_out.domain.model.ConvertShoppingCart;
import com.scale.check_out.domain.model.UpdateOrder;
import com.scale.domain.CannotCreateOrder;
import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;
import kong.unirest.HttpStatus;
import kong.unirest.Unirest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class OrderServiceUsingREST implements ConvertShoppingCart, UpdateOrder, ConfirmOrder {
    @NonNull String serviceHost;
    @NonNull Integer servicePort;

    @Override
    public Order intoOrder(ShoppingCart shoppingCart) {
        var response = Unirest.post(String.format("http://%s:%d/orders/convert", serviceHost, servicePort))
                .body(shoppingCart)
                .asObject(Order.class);

        if (response.getStatus() != HttpStatus.CREATED)
            throw new CannotCreateOrder(String.format("Error %d creating order", response.getStatus()));

        return response.getBody();
    }

    @Override
    public void changeAddress(Order order) {
        System.out.println("Not ready yet");
    }

    @Override
    public void handle(Order order) {
        System.out.println("Not ready yet");
    }

}
