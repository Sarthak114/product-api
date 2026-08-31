package com.zestindia.productapi.product;

import com.zestindia.productapi.common.api.PageResponse;
import com.zestindia.productapi.product.dto.ItemRequest;
import com.zestindia.productapi.product.dto.ItemResponse;
import com.zestindia.productapi.product.dto.ProductRequest;
import com.zestindia.productapi.product.dto.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List products with pagination")
    public ResponseEntity<PageResponse<ProductResponse>> list(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    public ResponseEntity<ProductResponse> get(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a product")
    public ResponseEntity<ProductResponse> update(@PathVariable Integer id,
                                                  @Valid @RequestBody ProductRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(productService.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a product")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        productService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "List items for a product")
    public ResponseEntity<List<ItemResponse>> listItems(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.findItems(id));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add an item to a product")
    public ResponseEntity<ItemResponse> addItem(@PathVariable Integer id,
                                                @Valid @RequestBody ItemRequest request,
                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addItem(id, request, authentication.getName()));
    }

    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an item quantity")
    public ResponseEntity<ItemResponse> updateItem(@PathVariable Integer id,
                                                   @PathVariable Integer itemId,
                                                   @Valid @RequestBody ItemRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(productService.updateItem(id, itemId, request, authentication.getName()));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an item from a product")
    public ResponseEntity<Void> deleteItem(@PathVariable Integer id,
                                           @PathVariable Integer itemId,
                                           Authentication authentication) {
        productService.deleteItem(id, itemId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
