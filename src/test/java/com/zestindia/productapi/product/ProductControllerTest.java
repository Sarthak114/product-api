package com.zestindia.productapi.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zestindia.productapi.common.api.PageResponse;
import com.zestindia.productapi.common.exception.GlobalExceptionHandler;
import com.zestindia.productapi.security.CustomUserDetailsService;
import com.zestindia.productapi.security.JwtService;
import com.zestindia.productapi.product.dto.ItemResponse;
import com.zestindia.productapi.product.dto.ProductRequest;
import com.zestindia.productapi.product.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@AutoConfigureMockMvc
@Import({ProductControllerTest.MethodSecurityTestConfig.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private ProductResponse sampleProduct() {
        return new ProductResponse(1, "Laptop", "admin", Instant.parse("2026-01-01T00:00:00Z"), null, null, 0);
    }

    @Test
    @WithMockUser(roles = "USER")
    void listProductsReturnsPage() throws Exception {
        when(productService.findAll(any(Pageable.class))).thenReturn(
                new PageResponse<>(List.of(sampleProduct()), 0, 10, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getProductById() throws Exception {
        when(productService.findById(1)).thenReturn(sampleProduct());

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createProductAsAdmin() throws Exception {
        when(productService.create(any(ProductRequest.class), eq("admin"))).thenReturn(sampleProduct());

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductRequest("Laptop"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Laptop"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProductAsUserIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductRequest("Laptop"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProductRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateAndDeleteProduct() throws Exception {
        when(productService.update(eq(1), any(ProductRequest.class), eq("admin"))).thenReturn(sampleProduct());

        mockMvc.perform(put("/api/v1/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductRequest("Laptop"))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/products/1").with(csrf()))
                .andExpect(status().isNoContent());
        verify(productService).delete(1, "admin");
    }

    @Test
    @WithMockUser(roles = "USER")
    void listItems() throws Exception {
        when(productService.findItems(1)).thenReturn(List.of(new ItemResponse(2, 1, 5)));

        mockMvc.perform(get("/api/v1/products/1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(5));
    }
}
