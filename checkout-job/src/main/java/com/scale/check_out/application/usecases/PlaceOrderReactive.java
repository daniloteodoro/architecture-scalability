package com.scale.check_out.application.usecases;

import com.scale.check_out.application.services.order.*;
import com.scale.check_out.application.services.payment.PayOrderReactive;
import com.scale.check_out.domain.metrics.BusinessMetrics;
import com.scale.domain.ShoppingCart;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@Slf4j
@AllArgsConstructor
public class PlaceOrderReactive {
    @NonNull ConvertShoppingCartReactive convertShoppingCart;
    @NonNull PayOrderReactive payOrder;
    @NonNull ConfirmOrderReactive confirmOrder;
    // TODO: Migrate to reactive ?
    @NonNull BusinessMetrics metrics;

    public Mono<Void> basedOn(ShoppingCart shoppingCart) {
        var start = System.currentTimeMillis();
        return convertShoppingCart.intoOrder(shoppingCart)
                .flatMap(order -> payOrder.with(shoppingCart.getClientId(), order))
                .flatMap(paymentReceiptDto -> confirmOrder.withPaymentReceipt(paymentReceiptDto))
                .onErrorMap(throwable -> new CannotConvertShoppingCart("Failure processing shopping cart: " + throwable.getMessage()))
                .doFinally(signalType -> {
                    if (signalType == SignalType.ON_COMPLETE)
                        log.info("Shopping cart processed");

                    var serviceTime = System.currentTimeMillis() - start;
                    var waitingTime = System.currentTimeMillis() - shoppingCart.getCreatedAt().toInstant().toEpochMilli();

                    metrics.finishShoppingCart(serviceTime, waitingTime, shoppingCart.getIsFirst(), shoppingCart.getIsLast());
                });
    }

}
