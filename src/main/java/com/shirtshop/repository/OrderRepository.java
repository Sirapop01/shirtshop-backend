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

    // ใช้ในหน้า “ประวัติการสั่งซื้อ” (แบ่งหน้า + เรียง)
    Page<Order> findByUserId(String userId, Pageable pageable);

    // กรองตามสถานะหลายค่า (เช่น PENDING_PAYMENT, SLIP_UPLOADED)
    Page<Order> findByUserIdAndStatusIn(String userId, List<OrderStatus> statuses, Pageable pageable);

    // ของเดิม: ใช้หาบิลล่าสุดที่ยังไม่หมดอายุ ในชุดสถานะที่กำหนด
    Optional<Order> findTopByUserIdAndStatusInAndExpiresAtAfterOrderByCreatedAtDesc(
            String userId, List<OrderStatus> statuses, Instant now
    );
}
