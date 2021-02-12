package com.scale.check_out.application.services.order;

import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;
import reactor.core.publisher.Mono;

public interface ConvertShoppingCartReactive {

    Mono<Order> intoOrder(ShoppingCart shoppingCart);

}
