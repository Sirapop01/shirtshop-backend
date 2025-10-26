// src/main/java/com/shirtshop/repository/OrderRepository.java
package com.shirtshop.repository;

import com.shirtshop.entity.Order;
import com.shirtshop.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {

    // Pageable, คืนค่าเป็น Page
    Page<Order> findByUserId(String userId, Pageable pageable);

    //  Pageable, คืนค่าเป็น Page
    Page<Order> findByUserIdAndStatusIn(String userId, List<OrderStatus> statuses, Pageable pageable);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    // ของเดิม: ใช้หาบิลล่าสุดที่ยังไม่หมดอายุ ในชุดสถานะที่กำหนด
    Optional<Order> findTopByUserIdAndStatusInAndExpiresAtAfterOrderByCreatedAtDesc(
            String userId, List<OrderStatus> statuses, Instant now
    );

    List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, Instant time);

}
