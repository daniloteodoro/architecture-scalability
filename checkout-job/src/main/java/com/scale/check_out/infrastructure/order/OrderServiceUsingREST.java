package com.scale.check_out.infrastructure.order;

import com.scale.check_out.application.services.payment.PaymentDto;
import com.scale.check_out.domain.model.order.ConfirmOrder;
import com.scale.check_out.domain.model.order.ConvertShoppingCart;
import com.scale.domain.*;
import kong.unirest.HttpStatus;
import kong.unirest.Unirest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class OrderServiceUsingREST implements ConvertShoppingCart, ConfirmOrder {
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
    public void withPaymentReceipt(Order order, PaymentDto.PaymentReceiptDto receipt) {

        var response = Unirest.put(String.format("http://%s:%d/orders/%s/confirm", serviceHost, servicePort, order.getId().value()))
                .body(receipt.getNumber())
                .asString();

        if (response.getStatus() != HttpStatus.NO_CONTENT)
            throw new CannotConfirmOrder(String.format("Error %d confirming order", response.getStatus()));
    }

}
