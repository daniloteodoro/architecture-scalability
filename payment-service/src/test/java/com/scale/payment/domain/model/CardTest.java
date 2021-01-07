package com.scale.payment.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class CardTest {
    final static String SAMPLE_ORDER_REFERENCE = "ABC1234";

    @Test
    public void testSameCard() {
        var first = new Card("1234", (short)333, new Card.ExpirationDate("10/2030"), Money.of(10.0));
        var second = new Card("1234", (short)333, new Card.ExpirationDate("10/2030"), Money.of(20.0));
        assertEquals(first, second);
    }

    @Test
    public void givenAnEmptyExpirationDateAnExceptionIsThrown() {
        Assertions.assertThrows(Card.CardError.class, () -> new Card.ExpirationDate("2030/10"));
    }

    @Test
    public void givenAnInvalidExpirationDateAnExceptionIsThrown() {
        Assertions.assertThrows(Card.CardError.class, () -> new Card.ExpirationDate("2030/10"));
    }

    @Test
    public void givenAValidExpirationDateTheObjectIsCreatedCorrectly() {
        new Card.ExpirationDate("10/2030").asDate().isEqual(LocalDate.of(2030, 10, 31));
    }

    @Test
    public void givenANewCardWhenCheckingForExpiredThenResultIsFalse() {
        var card = new Card("1234", (short)333, new Card.ExpirationDate("10/2030"), Money.of(3000.0));
        assertFalse(card.isExpired());
    }

    @Test
    public void givenAnOldCardWhenCheckingForExpiredThenResultIsTrue() {
        var card = new Card("1234", (short)333, new Card.ExpirationDate("10/2010"), Money.of(3000.0));
        assertTrue(card.isExpired());
    }

    @Test
    public void givenACardWithLowBalanceWhenCheckingLimitForABigAmountThenResultIsFalse() {
        var card = new Card("1234", (short)333, new Card.ExpirationDate("10/2030"), Money.of(10.0));
        assertFalse(card.hasLimitFor(Money.of(1000.00)));
    }

    @Test
    public void givenACardWhenCheckingLimitForASmallAmountThenResultIsTrue() {
        var card = new Card("1234", (short)333, new Card.ExpirationDate("10/2030"), Money.of(300.0));
        assertTrue(card.hasLimitFor(Money.of(50.00)));
    }

    @Test
    public void givenAValidCardWhenPayingAnOrderThenReceiptIsGeneratedCorrectly() {
        var card = new Card("1234", (short)333, new Card.ExpirationDate("10/2030"), Money.of(300.0));
        Card.Receipt receipt = card.pay(Money.of(150.0), SAMPLE_ORDER_REFERENCE);
        assertNotNull(receipt);
        assertThat(receipt.getNumber().length(), is(greaterThan(0)));
        assertThat(receipt.getReference(), is(equalTo(SAMPLE_ORDER_REFERENCE)));
        assertThat(receipt.getAmount().getValue(), is(closeTo(BigDecimal.valueOf(150.0), BigDecimal.valueOf(0.00001))));
        assertThat(card.getLimit().getValue(), is(closeTo(BigDecimal.valueOf(150.0), BigDecimal.valueOf(0.00001))));
    }

    @Test
    public void givenAnExpiredCardWhenTryingToPayThenExceptionIsThrown() {
        var card = new Card("1234", (short)333, new Card.ExpirationDate("10/2010"), Money.of(300.0));
        assertThrows(Card.CardError.class, () -> card.pay(Money.of(150.0), SAMPLE_ORDER_REFERENCE));
    }

    @Test
    public void givenAValidCardWhenTryingToPayMoreThanWhatIsAvailableThenExceptionIsThrown() {
        var card = new Card("1234", (short)333, new Card.ExpirationDate("10/2030"), Money.of(300.0));
        assertThrows(Card.CardError.class, () -> card.pay(Money.of(500.99), SAMPLE_ORDER_REFERENCE));
    }

}
