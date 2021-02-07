package com.scale.payment.domain.model;

import lombok.NonNull;
import lombok.Value;

@Value
public class ClientId {
    @NonNull String value;

    public boolean isValid() {
        return value != null;
    }

}
