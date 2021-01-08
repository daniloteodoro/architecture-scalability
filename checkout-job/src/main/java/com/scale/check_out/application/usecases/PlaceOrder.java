package com.scale.check_out.application.usecases;

import com.scale.check_out.application.services.payment.PayOrder;
import com.scale.check_out.domain.model.CannotConvertShoppingCart;
import com.scale.check_out.domain.model.order.ConfirmOrder;
import com.scale.check_out.domain.model.order.ConvertShoppingCart;
import com.scale.check_out.domain.metrics.BusinessMetrics;
import com.scale.domain.ShoppingCart;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class PlaceOrder {
    @NonNull ConvertShoppingCart convertShoppingCart;
    @NonNull PayOrder payOrder;
    @NonNull ConfirmOrder confirmOrder;
    @NonNull BusinessMetrics metrics;

    public void basedOn(ShoppingCart shoppingCart) {
        var start = System.currentTimeMillis();
        try {
            var order = convertShoppingCart.intoOrder(shoppingCart);

            var receipt = payOrder.with(shoppingCart.getClientId(), order);

            confirmOrder.withPaymentReceipt(order, receipt);

            log.info("Shopping cart processed");
        } catch (Exception e) {
            throw new CannotConvertShoppingCart("Failure processing shopping cart: " + e.getMessage());
        } finally {
            var serviceTime = System.currentTimeMillis() - start;
            var waitingTime = System.currentTimeMillis() - shoppingCart.getCreatedAt().toInstant().toEpochMilli();

            metrics.finishShoppingCart(serviceTime, waitingTime, shoppingCart.getIsFirst(), shoppingCart.getIsLast());
        }
    }

}
