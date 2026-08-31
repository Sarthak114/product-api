package com.zestindia.productapi.product.dto;

import com.zestindia.productapi.product.Product;

import java.time.Instant;

public record ProductResponse(
        Integer id,
        String productName,
        String createdBy,
        Instant createdOn,
        String modifiedBy,
        Instant modifiedOn,
        int itemCount
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductName(),
                product.getCreatedBy(),
                product.getCreatedOn(),
                product.getModifiedBy(),
                product.getModifiedOn(),
                product.getItems() == null ? 0 : product.getItems().size()
        );
    }
}
