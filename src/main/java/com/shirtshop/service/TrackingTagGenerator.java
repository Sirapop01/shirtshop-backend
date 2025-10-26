// src/main/java/com/shirtshop/service/TrackingTagGenerator.java
package com.shirtshop.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TrackingTagGenerator {
    private final AtomicInteger seq = new AtomicInteger(0);
    private final DateTimeFormatter d = DateTimeFormatter.BASIC_ISO_DATE; // YYYYMMDD

    public String nextTag(String prefix) {
        String day = LocalDate.now().format(d);
        int n = seq.updateAndGet(i -> (i >= 99999 ? 1 : i + 1));
        return "%s-%s-%05d".formatted(prefix, day, n); // e.g. TAG-20251023-00012
    }
}
