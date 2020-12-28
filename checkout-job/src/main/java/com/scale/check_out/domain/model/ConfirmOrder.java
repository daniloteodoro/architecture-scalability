package com.scale.check_out.domain.model;

import com.scale.domain.Order;

public interface ConfirmOrder {

    void handle(Order order);

}
