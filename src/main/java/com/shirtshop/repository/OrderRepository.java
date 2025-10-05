// src/main/java/com/shirtshop/repository/OrderRepository.java
package com.shirtshop.repository;

import com.shirtshop.entity.Order;
import com.shirtshop.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    Page<Order> findByUserId(String userId, Pageable pageable);
    Page<Order> findByUserIdAndStatusIn(String userId, List<OrderStatus> statuses, Pageable pageable);
}
