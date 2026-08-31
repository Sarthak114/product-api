package com.zestindia.productapi.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemRequest(
        @NotNull @Min(1) Integer quantity
) {
}
