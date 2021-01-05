package com.scale.payment.application.usecases;

import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.model.Receipt;
import com.scale.payment.domain.repository.PaymentRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;

@RequiredArgsConstructor
@Slf4j
public class PayOrder {
    @NonNull PaymentRepository paymentRepository;

    /**
     * Check whether the given card has a positive balance - after subtracting the amount.
     * Then subtracts the amount from the card's balance and register the payment operation.
     * We assume all operations are in Euros - so no need for conversions.
     * @param id     The order id to be associated with the payment
     * @param amount The amount to be debited from the given card.
     * @param card   The card to be debited.
     * @return The receipt of the payment operation.
     */
    public Receipt using(Order.OrderId id, Money amount, Card card) {
        var possiblePayment = paymentRepository.getReceipt(id, amount);
        if (possiblePayment.isPresent()) {
            return possiblePayment.get();
        }

        card.pay(amount, id.value());

//        !card.isValid (expired)
//        !card.hasLimitFor(amount)
//
//        subtract <amount> from limit
//        register payment and associate with order_id, amount, card_nr
//        generate receipt: Receipt.generate()
//        var order = paymentRepository.add(receipt)
//
//        Put above operation inside Domain objects as much as possible to allow better testing

        // order.confirm();
        // orderRepository.update(order);

        return Receipt.generate();
    }

}
