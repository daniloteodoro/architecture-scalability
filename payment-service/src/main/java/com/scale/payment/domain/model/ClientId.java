package com.scale.payment.domain.model;

import lombok.NonNull;
import lombok.Value;

@Value
public class ClientId {
    @NonNull String value;

    public boolean isValid() {
        return value != null;
    }

    public static class ClientNotFound extends PaymentError {
        public ClientNotFound(String msg) {
            super(msg);
        }
    }

}
