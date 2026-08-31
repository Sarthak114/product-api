package com.zestindia.productapi.product;

import com.zestindia.productapi.common.api.PageResponse;
import com.zestindia.productapi.common.audit.AuditService;
import com.zestindia.productapi.common.exception.ResourceNotFoundException;
import com.zestindia.productapi.product.dto.ItemRequest;
import com.zestindia.productapi.product.dto.ItemResponse;
import com.zestindia.productapi.product.dto.ProductRequest;
import com.zestindia.productapi.product.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ItemRepository itemRepository;
    private final AuditService auditService;

    public ProductService(ProductRepository productRepository,
                          ItemRepository itemRepository,
                          AuditService auditService) {
        this.productRepository = productRepository;
        this.itemRepository = itemRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(Pageable pageable) {
        Page<ProductResponse> page = productRepository.findAll(pageable).map(ProductResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Integer id) {
        return ProductResponse.from(getProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request, String actor) {
        Product product = new Product();
        product.setProductName(request.productName().trim());
        product.setCreatedBy(actor);
        product.setCreatedOn(Instant.now());
        Product saved = productRepository.save(product);
        auditService.record(actor, "CREATE", "product:" + saved.getId(), saved.getProductName());
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(Integer id, ProductRequest request, String actor) {
        Product product = getProduct(id);
        product.setProductName(request.productName().trim());
        product.setModifiedBy(actor);
        product.setModifiedOn(Instant.now());
        Product saved = productRepository.save(product);
        auditService.record(actor, "UPDATE", "product:" + id, saved.getProductName());
        return ProductResponse.from(saved);
    }

    @Transactional
    public void delete(Integer id, String actor) {
        Product product = getProduct(id);
        productRepository.delete(product);
        auditService.record(actor, "DELETE", "product:" + id, product.getProductName());
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> findItems(Integer productId) {
        getProduct(productId);
        return itemRepository.findByProductId(productId).stream()
                .map(ItemResponse::from)
                .toList();
    }

    @Transactional
    public ItemResponse addItem(Integer productId, ItemRequest request, String actor) {
        Product product = getProduct(productId);
        Item item = new Item();
        item.setProduct(product);
        item.setQuantity(request.quantity());
        Item saved = itemRepository.save(item);
        product.setModifiedBy(actor);
        product.setModifiedOn(Instant.now());
        productRepository.save(product);
        auditService.record(actor, "CREATE", "item:" + saved.getId(), "product=" + productId);
        return ItemResponse.from(saved);
    }

    @Transactional
    public ItemResponse updateItem(Integer productId, Integer itemId, ItemRequest request, String actor) {
        getProduct(productId);
        Item item = itemRepository.findByIdAndProductId(itemId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));
        item.setQuantity(request.quantity());
        Item saved = itemRepository.save(item);
        auditService.record(actor, "UPDATE", "item:" + itemId, "quantity=" + request.quantity());
        return ItemResponse.from(saved);
    }

    @Transactional
    public void deleteItem(Integer productId, Integer itemId, String actor) {
        getProduct(productId);
        Item item = itemRepository.findByIdAndProductId(itemId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));
        itemRepository.delete(item);
        auditService.record(actor, "DELETE", "item:" + itemId, "product=" + productId);
    }

    private Product getProduct(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
}
