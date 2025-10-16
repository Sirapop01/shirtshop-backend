// src/main/java/com/shirtshop/controller/CompatController.java
package com.shirtshop.controller;

import com.shirtshop.dto.DashboardSummaryResponse;
import com.shirtshop.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CompatController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public DashboardSummaryResponse dashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        if (from == null) {
            ZoneId tz = ZoneId.of("Asia/Bangkok");
            from = LocalDate.now(tz).atStartOfDay(tz).toInstant();
        }
        if (to == null) to = Instant.now();
        if (from.isAfter(to)) { var t = from; from = to; to = t; }
        return analyticsService.findSummary(from, to);
    }
}
