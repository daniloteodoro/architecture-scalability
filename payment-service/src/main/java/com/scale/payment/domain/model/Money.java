package com.scale.payment.domain.model;

import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Take care of amounts of money. For now we skipped implementing currencies and rounding as explained here: https://martinfowler.com/eaaCatalog/money.html
 */
@Value
public class Money {
    @NonNull BigDecimal value;

    private Money(@NonNull BigDecimal val) {
        if (val.doubleValue() < 0)
            throw new InvalidMoneyAmount("Value must be greater than or equal to zero");
        this.value = val;
    }

    public static Money of(Double val) {
        return new Money(BigDecimal.valueOf(val));
    }

    public Money subtract(Money amount) {
        // Constructor takes care of validation
        return new Money(BigDecimal.valueOf(this.value.doubleValue() - amount.value.doubleValue()));
    }

    public static class InvalidMoneyAmount extends PaymentError {
        public InvalidMoneyAmount(String msg) {
            super(msg);
        }
    }
}
