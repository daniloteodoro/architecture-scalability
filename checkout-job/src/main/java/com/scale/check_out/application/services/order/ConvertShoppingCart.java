package com.scale.check_out.application.services.order;

import com.scale.domain.Order;
import com.scale.domain.ShoppingCart;

public interface ConvertShoppingCart {

    Order intoOrder(ShoppingCart shoppingCart);

}
