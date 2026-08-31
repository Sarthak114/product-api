package com.zestindia.productapi.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Integer> {

    List<Item> findByProductId(Integer productId);

    Optional<Item> findByIdAndProductId(Integer id, Integer productId);
}
