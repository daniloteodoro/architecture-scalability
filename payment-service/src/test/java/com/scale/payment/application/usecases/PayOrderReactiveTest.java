package com.scale.payment.application.usecases;

import com.scale.domain.Order;
import com.scale.payment.domain.model.Card;
import com.scale.payment.domain.model.ClientId;
import com.scale.payment.domain.model.Money;
import com.scale.payment.infrastructure.repository.PaymentReactiveRepositoryInMemory;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class PayOrderReactiveTest {

    @Test
    public void testSuccessfulPayment() {
        var repo = new PaymentReactiveRepositoryInMemory();
        repo.insertDefaultClientsWithCards();

        var payOrderUseCase = new PayOrderReactive(repo);
        Mono<Card.Receipt> receipt =
                payOrderUseCase.usingClientCard(new ClientId("5ff878f5e77e950006a814b3"), Order.OrderId.generateNew(), Money.of(199.99));

        StepVerifier
                .create(receipt)
                .expectNextMatches(result -> (result.getClass().equals(Card.Receipt.class)) &&
                        result.getCard().getNumber().equalsIgnoreCase("5ff873c1e77e950006a814af") &&
                        result.getAmount().equals(Money.of(199.99)))
                .expectComplete()
                .verify();
    }

    @Test
    public void testPayingTwiceOnlyChargesOnce() {
        var repo = new PaymentReactiveRepositoryInMemory();
        repo.insertDefaultClientsWithCards();

        PayOrderReactive payOrderUseCase = new PayOrderReactive(repo);
        Order.OrderId orderReference = Order.OrderId.generateNew();

        Mono<Card.Receipt> receipt =
                payOrderUseCase.usingClientCard(new ClientId("5ff878f5e77e950006a814b3"), orderReference, Money.of(199.99))
                .then(payOrderUseCase.usingClientCard(new ClientId("5ff878f5e77e950006a814b3"), orderReference, Money.of(199.99)));

        StepVerifier.create(receipt)
                .expectNextMatches(result -> result instanceof Card.OrderAlreadyPaidReceipt &&
                        result.getCard().getNumber().equalsIgnoreCase("5ff873c1e77e950006a814af") &&
                        result.getAmount().equals(Money.of(199.99)))
                .expectComplete()
                .verify();
    }

    @Test
    public void testTryingToPayWithUnknownClient() {
        var repo = new PaymentReactiveRepositoryInMemory();
        repo.insertDefaultClientsWithCards();

        PayOrderReactive payOrderUseCase = new PayOrderReactive(repo);
        Order.OrderId orderReference = Order.OrderId.generateNew();

        StepVerifier.create(payOrderUseCase.usingClientCard(new ClientId("someone"), orderReference, Money.of(199.99)))
                .expectError(ClientId.ClientNotFound.class)
                .verify();
    }

    @Test
    public void testTryingToPayInsufficientFunds() {
        var repo = new PaymentReactiveRepositoryInMemory();
        repo.insertDefaultClientsWithCards();

        PayOrderReactive payOrderUseCase = new PayOrderReactive(repo);
        Order.OrderId orderReference = Order.OrderId.generateNew();

        StepVerifier.create(payOrderUseCase.usingClientCard(new ClientId("5ff878f5e77e950006a814b3"), orderReference, Money.of(Integer.MAX_VALUE*1.0)))
                .expectError(Card.InsufficientFunds.class)
                .verify();
    }

}
