package com.scale.payment.domain.model;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

@Value
public class Card {
    @NonNull String number;
    @NonNull Short digit;
    @NonNull ExpirationDate ExpirationDate;
    @NonNull @NonFinal Money limit;

    public void pay(Money amount, String reference) {
        if (this.isExpired())
            throw new CardError("This card is expired");

        if (!hasLimitFor(amount))
            throw new CardError("Insufficient funds");

        this.limit = limit.subtract(amount);

//        register payment and associate with order_id, amount, card_nr
//        generate receipt: Receipt.generate()
//        var order = paymentRepository.add(receipt)

    }

    public boolean hasLimitFor(Money amount) {
        try {
            limit.subtract(amount);
            return true;
        } catch (Money.InvalidMoneyAmount negativeBalance) {
            return false;
        }
    }

    public boolean isExpired() {
        var expiredAt = getExpirationDate().asDate();
        return expiredAt.isBefore(LocalDate.now());
    }

    @Value
    public static class ExpirationDate {
        @NonNull String value;

        public ExpirationDate(@NonNull String val) {
            if (val.length() != 7 || val.charAt(2) != '/')
                throw new CardError("Expiration date should be in format MM/YYYY");
            this.value = val;
        }

        public LocalDate asDate() {
            String date = String.format("01/%s", value);
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    .with(TemporalAdjusters.lastDayOfMonth());
        }
    }

    public static class CardError extends RuntimeException {
        public CardError(String msg) {
            super(msg);
        }
    }

}
