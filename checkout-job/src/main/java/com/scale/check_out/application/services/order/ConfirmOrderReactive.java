package com.scale.check_out.application.services.order;

import com.scale.check_out.application.services.payment.PaymentDto;
import reactor.core.publisher.Mono;

public interface ConfirmOrderReactive {

    Mono<Void> withPaymentReceipt(PaymentDto.PaymentReceiptDto receipt);

}
