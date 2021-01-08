package com.scale.check_out.application.services.payment;

import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;

public interface PayOrder {

    /**
     * Execute a payment for the given order using the card associated with the client.
     * @param clientId The already registered client, which must have an associated card.
     * @param order    The order to be paid
     * @return A receipt of the payment, in case of success, otherwise an exception is thrown.
     */
    PaymentDto.PaymentReceiptDto with(ShoppingCart.ClientId clientId, Order order);

}
