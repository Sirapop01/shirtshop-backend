// src/main/java/com/shirtshop/repository/CartRepository.java
package com.shirtshop.repository;

import com.shirtshop.entity.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByUserId(String userId);
}
