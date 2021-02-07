package com.scale.payment.application.usecases;

import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentReactiveRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class PayOrderReactive {
    @NonNull PaymentReactiveRepository paymentRepository;

    /**
     * Check whether the given card has a positive balance - after subtracting the amount.
     * Then subtracts the amount from the card's balance and register the payment operation.
     * We assume all operations are in Euros - so no need for conversions.
     * @param id     The order id to be associated with the payment
     * @param amount The amount to be debited from the given card.
     * @param card   The card to be debited.
     * @return The receipt of the payment operation.
     */
    public Mono<Card.Receipt> using(Card card, Order.OrderId id, Money amount) {
        return paymentRepository.findReceipt(id, amount)
                .flatMap(this::generateExistingPaymentForReceipt)
                // TODO: This can only be called after knowing the result (defer)!!!
                .switchIfEmpty(generateNewPaymentFor(card, id, amount));
    }

    private Mono<Card.Receipt> generateNewPaymentFor(Card card, Order.OrderId id, Money amount) {
        try {
            var receipt = card.pay(amount, id.value());
            return paymentRepository.addReceipt(receipt);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private Mono<Card.Receipt> generateExistingPaymentForReceipt(Card.Receipt receipt) {
        return Mono.just(new Card.OrderAlreadyPaidReceipt(receipt));
    }

}
