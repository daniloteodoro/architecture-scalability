package com.scale.payment.application.usecases;

import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.domain.repository.PaymentRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    public Card.Receipt using(ClientId clientId, Order.OrderId id, Money amount) {
        var card = paymentRepository.findCardByClient(clientId)
                .orElseThrow(() -> new ClientId.ClientNotFound(String.format("Client %s was not found or has no associated card", clientId.getValue())));

        var possiblePayment = paymentRepository.findReceipt(id, amount);
        if (possiblePayment.isPresent()) {
            return new Card.OrderAlreadyPaidReceipt(possiblePayment.get());
        }

        var receipt = card.pay(amount, id.value());

        paymentRepository.addReceipt(receipt);

        return receipt;
    }

}
