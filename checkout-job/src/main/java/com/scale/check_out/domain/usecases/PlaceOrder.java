package com.scale.check_out.domain.usecases;

import com.scale.check_out.domain.model.CannotConvertShoppingCart;
import com.scale.check_out.domain.model.ConfirmOrder;
import com.scale.check_out.domain.model.ConvertShoppingCart;
import com.scale.check_out.domain.model.UpdateOrder;
import com.scale.check_out.domain.metrics.BusinessMetrics;
import com.scale.domain.ShoppingCart;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class PlaceOrder {
    @NonNull ConvertShoppingCart convertShoppingCart;
    @NonNull UpdateOrder updateOrder;
    @NonNull ConfirmOrder confirmOrder;
    @NonNull BusinessMetrics metrics;

    public void basedOn(ShoppingCart shoppingCart) {
        var start = System.currentTimeMillis();
        try {
            var order = convertShoppingCart.intoOrder(shoppingCart);
            updateOrder.changeAddress(order);
            // TODO: Handle Payment
            confirmOrder.handle(order);

            log.info("Shopping cart processed");
        } catch (Exception e) {
            throw new CannotConvertShoppingCart("Failure converting shopping cart: " + e.getMessage());
        } finally {
            var serviceTime = System.currentTimeMillis() - start;
            var waitingTime = System.currentTimeMillis() - shoppingCart.getCreatedAt().toInstant().toEpochMilli();

            metrics.finishShoppingCart(serviceTime, waitingTime, shoppingCart.getIsFirst(), shoppingCart.getIsLast());
        }
    }

}
