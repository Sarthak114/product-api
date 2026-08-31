package com.zestindia.productapi.product;

import com.zestindia.productapi.common.api.PageResponse;
import com.zestindia.productapi.common.audit.AuditService;
import com.zestindia.productapi.common.exception.ResourceNotFoundException;
import com.zestindia.productapi.product.dto.ItemRequest;
import com.zestindia.productapi.product.dto.ItemResponse;
import com.zestindia.productapi.product.dto.ProductRequest;
import com.zestindia.productapi.product.dto.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1);
        product.setProductName("Laptop");
        product.setCreatedBy("admin");
        product.setCreatedOn(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void findAllReturnsPagedResponse() {
        when(productRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1));

        PageResponse<ProductResponse> response = productService.findAll(PageRequest.of(0, 10));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).productName()).isEqualTo("Laptop");
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.first()).isTrue();
    }

    @Test
    void findByIdReturnsProduct() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        ProductResponse response = productService.findById(1);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.productName()).isEqualTo("Laptop");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createPersistsAuditFields() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(10);
            return saved;
        });

        ProductResponse response = productService.create(new ProductRequest("  Keyboard  "), "admin");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getProductName()).isEqualTo("Keyboard");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("admin");
        assertThat(captor.getValue().getCreatedOn()).isNotNull();
        assertThat(response.id()).isEqualTo(10);
        verify(auditService).record(eq("admin"), eq("CREATE"), eq("product:10"), eq("Keyboard"));
    }

    @Test
    void updateChangesNameAndModifier() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.update(1, new ProductRequest("Monitor"), "admin");

        assertThat(response.productName()).isEqualTo("Monitor");
        assertThat(response.modifiedBy()).isEqualTo("admin");
        assertThat(response.modifiedOn()).isNotNull();
    }

    @Test
    void deleteRemovesProduct() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        productService.delete(1, "admin");

        verify(productRepository).delete(product);
        verify(auditService).record("admin", "DELETE", "product:1", "Laptop");
    }

    @Test
    void findItemsReturnsMappedResponses() {
        Item item = new Item();
        item.setId(5);
        item.setQuantity(3);
        item.setProduct(product);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(itemRepository.findByProductId(1)).thenReturn(List.of(item));

        List<ItemResponse> items = productService.findItems(1);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).quantity()).isEqualTo(3);
        assertThat(items.get(0).productId()).isEqualTo(1);
    }

    @Test
    void addItemAttachesToProduct() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item saved = invocation.getArgument(0);
            saved.setId(7);
            return saved;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItemResponse response = productService.addItem(1, new ItemRequest(4), "admin");

        assertThat(response.id()).isEqualTo(7);
        assertThat(response.quantity()).isEqualTo(4);
    }

    @Test
    void updateItemThrowsWhenMissing() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(itemRepository.findByIdAndProductId(8, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateItem(1, 8, new ItemRequest(2), "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
