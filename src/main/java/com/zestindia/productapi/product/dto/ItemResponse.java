package com.zestindia.productapi.product.dto;

import com.zestindia.productapi.product.Item;

public record ItemResponse(
        Integer id,
        Integer productId,
        Integer quantity
) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getProduct().getId(), item.getQuantity());
    }
}
