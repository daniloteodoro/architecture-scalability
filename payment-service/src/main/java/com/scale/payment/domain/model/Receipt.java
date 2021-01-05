package com.scale.payment.domain.model;

import com.scale.domain.Order;
import lombok.NonNull;
import lombok.Value;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Value
public class Receipt {
    @NonNull String number;
    @NonNull ZonedDateTime time;

    public static Receipt generate() {
        return new Receipt(UUID.randomUUID().toString(), ZonedDateTime.now(ZoneOffset.UTC));
    }

    private Receipt(@NonNull String number, @NonNull ZonedDateTime time) {
        this.number = number;
        this.time = time;
    }
}
