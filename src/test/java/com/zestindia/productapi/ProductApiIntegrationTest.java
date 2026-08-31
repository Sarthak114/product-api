package com.zestindia.productapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zestindia.productapi.auth.dto.LoginRequest;
import com.zestindia.productapi.auth.dto.RefreshTokenRequest;
import com.zestindia.productapi.auth.dto.RegisterRequest;
import com.zestindia.productapi.product.dto.ItemRequest;
import com.zestindia.productapi.product.dto.ProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unauthenticatedProductAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void adminCanPerformFullProductAndItemCrud() throws Exception {
        String adminToken = login("admin", "Admin@123");

        MvcResult createResult = mockMvc.perform(authorized(post("/api/v1/products"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductRequest("Wireless Mouse"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Wireless Mouse"))
                .andExpect(jsonPath("$.createdBy").value("admin"))
                .andReturn();

        int productId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(authorized(get("/api/v1/products/" + productId), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId));

        mockMvc.perform(authorized(get("/api/v1/products?page=0&size=5"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));

        mockMvc.perform(authorized(put("/api/v1/products/" + productId), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductRequest("Ergonomic Mouse"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Ergonomic Mouse"))
                .andExpect(jsonPath("$.modifiedBy").value("admin"));

        MvcResult itemResult = mockMvc.perform(authorized(post("/api/v1/products/" + productId + "/items"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemRequest(12))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(12))
                .andReturn();
        int itemId = objectMapper.readTree(itemResult.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(authorized(get("/api/v1/products/" + productId + "/items"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(itemId));

        mockMvc.perform(authorized(delete("/api/v1/products/" + productId), adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(authorized(get("/api/v1/products/" + productId), adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: " + productId));
    }

    @Test
    void userCanReadButCannotMutateProducts() throws Exception {
        String adminToken = login("admin", "Admin@123");
        MvcResult createResult = mockMvc.perform(authorized(post("/api/v1/products"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductRequest("USB Hub"))))
                .andExpect(status().isCreated())
                .andReturn();
        int productId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        String userToken = login("user", "User@123");
        mockMvc.perform(authorized(get("/api/v1/products/" + productId), userToken))
                .andExpect(status().isOk());
        mockMvc.perform(authorized(post("/api/v1/products"), userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductRequest("Forbidden"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(authorized(delete("/api/v1/products/" + productId), userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerLoginAndRefreshTokenRotation() throws Exception {
        String username = "newuser_" + System.nanoTime();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(username, username + "@zestindia.local", "Passw0rd1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("USER"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, "Passw0rd1"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tokens = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = tokens.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String rotated = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("refreshToken").asText();
        assertThat(rotated).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authorized(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
