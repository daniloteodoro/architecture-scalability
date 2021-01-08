package com.scale.check_out.application.services.payment;

import com.scale.domain.ShoppingCart;
import kong.unirest.Client;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;

public class PaymentDto {

    @Value
    public static class PaymentReceiptDto {
        @NonNull String number;
        @NonNull ZonedDateTime time;
        @NonNull String reference;
        @NonNull Double amount;
    }

    @Value
    public static class PaymentRequestDto {
        @NonNull Double amount;
        @NonNull String orderId;
        @NonNull ShoppingCart.ClientId clientId;
    }

//    // TODO: Remove
//    @Value
//    public static class CardDetailsDto {
//        @NonNull String number;
//        @NonNull Short digit;
//        // MM/YYYY
//        @NonNull String expirationDate;
//    }

}
