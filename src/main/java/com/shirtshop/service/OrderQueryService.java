// src/main/java/com/shirtshop/service/OrderQueryService.java
package com.shirtshop.service;

import com.shirtshop.dto.OrderSummaryResponse;
import com.shirtshop.dto.PageResponse;
import com.shirtshop.entity.OrderStatus;
import com.shirtshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepo;

    private String currentUserId() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        Object p = a != null ? a.getPrincipal() : null;
        if (p instanceof com.shirtshop.entity.User u) return u.getId();
        if (p instanceof String s) return s;
        throw new IllegalStateException("Unauthenticated");
    }

    public PageResponse<OrderSummaryResponse> myOrders(List<OrderStatus> statuses, int page, int size) {
        String userId = currentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        var pageData = (statuses == null || statuses.isEmpty())
                ? orderRepo.findByUserId(userId, pageable)
                : orderRepo.findByUserIdAndStatusIn(userId, statuses, pageable);

        var mapped = pageData.map(o -> new OrderSummaryResponse(
                o.getId(),
                o.getCreatedAt(),
                o.getExpiresAt(),
                o.getTotal(),
                o.getItems() != null ? o.getItems().size() : 0,
                o.getPaymentMethod(),
                o.getStatus(),
                o.getPaymentSlipUrl()
        ));

        return new PageResponse<>(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages()
        );
    }
}
