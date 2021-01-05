package com.scale.payment.domain.model;

import com.scale.domain.CannotConvertShoppingCart;
import com.scale.domain.Product;
import com.scale.domain.ShoppingCart;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class CardTest {

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

}
