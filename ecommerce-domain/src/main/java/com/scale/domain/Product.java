package com.scale.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;

@Builder
@Value
public class Product {
        @NonNull Long id;
        @NonNull String name;
        @NonNull BigDecimal price;
        @NonNull Long inStock;

}
