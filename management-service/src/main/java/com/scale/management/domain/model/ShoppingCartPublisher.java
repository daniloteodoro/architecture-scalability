package com.scale.management.domain.model;

import com.scale.domain.ShoppingCart;

// Port to publish sample shopping carts
public interface ShoppingCartPublisher {

    void publish(ShoppingCart shoppingCart);

}
