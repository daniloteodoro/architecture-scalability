package com.scale.check_out.application.services.order;

import com.scale.check_out.application.services.payment.PaymentDto;
import com.scale.domain.Order;

public interface ConfirmOrder {

    void withPaymentReceipt(Order order, PaymentDto.PaymentReceiptDto receipt);

}
