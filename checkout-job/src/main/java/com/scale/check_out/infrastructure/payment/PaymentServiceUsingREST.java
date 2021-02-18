package com.scale.check_out.infrastructure.payment;

import com.google.gson.Gson;
import com.scale.check_out.application.services.payment.PayOrder;
import com.scale.check_out.application.services.payment.PaymentDto;
import com.scale.check_out.infrastructure.configuration.SerializerConfig;
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
public class PaymentServiceUsingREST implements PayOrder {
    @NonNull String serviceHost;
    @NonNull Integer servicePort;

    private final Gson gson = SerializerConfig.buildSerializer();

    @Override
    public PaymentDto.PaymentReceiptDto with(ShoppingCart.ClientId clientId, Order order) {
        var paymentRequest =
                new PaymentDto.PaymentRequestDto(order.calculateTotal(), order.getId().value(), clientId);

        var response = Unirest.post(String.format("http://%s:%d/payments", serviceHost, servicePort))
                .body(paymentRequest)
                .asObject(PaymentDto.PaymentReceiptDto.class);

        if (response.getStatus() != HttpStatus.CREATED && response.getStatus() != HttpStatus.OK)
            throw new CannotCreateOrder(String.format("Error %d paying order", response.getStatus()));

        return response.getBody();
    }

}
