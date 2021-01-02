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

    public Product(@NonNull Long id, @NonNull String name, @NonNull BigDecimal price, @NonNull Long inStock) {
        if (name.isBlank())
            throw new CannotCreateProduct("Product name cannot be blank");
        if (price.doubleValue() <= 0)
            throw new CannotCreateProduct("Product price must be greater than zero for id " + id);
        this.id = id;
        this.name = name;
        this.price = price;
        this.inStock = inStock;
    }
}
