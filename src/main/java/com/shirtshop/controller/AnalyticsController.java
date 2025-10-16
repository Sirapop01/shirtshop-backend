// src/main/java/com/shirtshop/controller/AnalyticsController.java
package com.shirtshop.controller;

import com.shirtshop.dto.TopProductResponse;
import com.shirtshop.dto.DashboardSummaryResponse;
import com.shirtshop.dto.CategoryBreakdownResponse;
import com.shirtshop.service.AnalyticsService;
import com.shirtshop.dto.RevenuePointResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.time.*;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/top-products")
    public List<TopProductResponse> topProducts(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, defaultValue = "7") Integer lastDays,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "REVENUE") AnalyticsService.SortBy sortBy
    ) {
        if (from != null && to == null) to = Instant.now();

        if (from == null || to == null) {
            if (lastDays != null && lastDays == 0) {
                ZoneId tz = ZoneId.of("Asia/Bangkok");
                from = LocalDate.now(tz).atStartOfDay(tz).toInstant();
                to = Instant.now();
            } else {
                int days = (lastDays == null || lastDays < 1) ? 7 : lastDays;
                to = Instant.now();
                from = to.minus(days, ChronoUnit.DAYS);
            }
        }
        if (from.isAfter(to)) { Instant t = from; from = to; to = t; }

        return analyticsService.findTopProducts(from, to, limit, sortBy);
    }

    // Summary: Revenue / Orders / AOV
    @GetMapping("/summary")
    public DashboardSummaryResponse summary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, defaultValue = "7") Integer lastDays
    ) {
        if (from != null && to == null) to = Instant.now();

        if (from == null || to == null) {
            if (lastDays != null && lastDays == 0) {
                ZoneId tz = ZoneId.of("Asia/Bangkok");
                from = LocalDate.now(tz).atStartOfDay(tz).toInstant();
                to = Instant.now();
            } else {
                int days = (lastDays == null || lastDays < 1) ? 7 : lastDays;
                to = Instant.now();
                from = to.minus(days, ChronoUnit.DAYS);
            }
        }
        if (from.isAfter(to)) { Instant t = from; from = to; to = t; }

        return analyticsService.findSummary(from, to);
    }

    // NEW: Sales by Category (count quantity of PAID orders)
    @GetMapping("/sales/by-category")
    public List<CategoryBreakdownResponse> salesByCategory(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, defaultValue = "7") Integer lastDays
    ) {
        if (from != null && to == null) to = Instant.now();

        if (from == null || to == null) {
            if (lastDays != null && lastDays == 0) {
                ZoneId tz = ZoneId.of("Asia/Bangkok");
                from = LocalDate.now(tz).atStartOfDay(tz).toInstant();
                to = Instant.now();
            } else {
                int days = (lastDays == null || lastDays < 1) ? 7 : lastDays;
                to = Instant.now();
                from = to.minus(days, ChronoUnit.DAYS);
            }
        }
        if (from.isAfter(to)) { Instant t = from; from = to; to = t; }

        return analyticsService.findSalesByCategory(from, to);
    }

    @GetMapping("/revenue/daily")
    public List<RevenuePointResponse> revenueDaily(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, defaultValue = "7") Integer lastDays
    ) {
        ZoneId tz = ZoneId.of("Asia/Bangkok");

        if (from != null && to == null) to = Instant.now();

        if (from == null || to == null) {
            if (lastDays != null && lastDays == 0) {
                // Today
                from = LocalDate.now(tz).atStartOfDay(tz).toInstant();
                to   = Instant.now();
            } else {
                int days = (lastDays == null || lastDays < 1) ? 7 : lastDays;
                to   = Instant.now();
                from = to.minus(days, ChronoUnit.DAYS);
            }
        }
        if (from.isAfter(to)) { Instant t = from; from = to; to = t; }

        return analyticsService.revenueDaily(from, to, tz);
    }
}
