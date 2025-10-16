// src/main/java/com/shirtshop/service/AnalyticsService.java
package com.shirtshop.service;

import com.shirtshop.dto.TopProductResponse;
import com.shirtshop.dto.DashboardSummaryResponse;
import com.shirtshop.dto.CategoryBreakdownResponse;
import com.shirtshop.entity.Order;
import com.shirtshop.dto.RevenuePointResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.time.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    public enum SortBy { REVENUE, UNITS }

    private final MongoTemplate mongoTemplate;

    // <<< ปรับชื่อตามคอลเลกชันสินค้าของคุณ ("product" หรือ "products")
    private static final String PRODUCTS_COLLECTION = "products";
    // >>> ---------------------------------------------

    /* =========================
     *   TOP PRODUCTS (เดิม)
     * ========================= */
    @SuppressWarnings("unchecked")
    public List<TopProductResponse> findTopProducts(Instant from, Instant to, int limit, SortBy sortBy) {
        String collection = mongoTemplate.getCollectionName(Order.class);

        Criteria cPaid = Criteria.where("status").is("PAID");
        Criteria cTime = new Criteria().orOperator(
                Criteria.where("paidAt").gte(from).lt(to),
                Criteria.where("updatedAt").gte(from).lt(to),
                Criteria.where("createdAt").gte(from).lt(to)
        );

        Query q = new Query(cPaid).addCriteria(cTime);
        q.fields()
                .include("items")
                .include("paidAt").include("updatedAt").include("createdAt");

        List<Document> orders = mongoTemplate.find(q, Document.class, collection);
        log.debug("[TopProducts] from={} to={} | matchedOrders={}", from, to, orders.size());

        Map<String, Acc> acc = new HashMap<>();

        for (Document od : orders) {
            List<Document> items = (List<Document>) od.get("items");
            if (items == null) continue;

            for (Document it : items) {
                String pid   = str(it.get("productId"));
                String name  = str(it.get("name"));  // snapshot ชื่อจากรายการ
                long qty     = toLong(it.get("quantity"));
                BigDecimal price = toBigDecimal(it.get("unitPrice"));

                if (pid == null || pid.isBlank() || qty <= 0) continue;

                BigDecimal line = price.multiply(BigDecimal.valueOf(qty));

                Acc a = acc.computeIfAbsent(pid, k -> new Acc());
                if (a.name == null || a.name.isBlank()) a.name = name;
                a.units += qty;
                a.revenue = a.revenue.add(line);
            }
        }

        Comparator<Map.Entry<String, Acc>> cmp =
                (sortBy == SortBy.UNITS)
                        ? Comparator.comparingLong((Map.Entry<String, Acc> e) -> e.getValue().units).reversed()
                        : Comparator.comparing((Map.Entry<String, Acc> e) -> e.getValue().revenue).reversed();

        List<TopProductResponse> out = acc.entrySet().stream()
                .sorted(cmp)
                .limit(Math.max(1, limit))
                .map(e -> TopProductResponse.builder()
                        .id(e.getKey())
                        .name((e.getValue().name == null || e.getValue().name.isBlank()) ? "Unknown" : e.getValue().name)
                        .units(e.getValue().units)
                        .revenue(e.getValue().revenue)
                        .build())
                .collect(Collectors.toList());

        log.debug("[TopProducts] resultSize={} first={}", out.size(), out.isEmpty() ? null : out.get(0));
        return out;
    }

    /* ==================================================
     *  SALES BY CATEGORY – รวม qty ของออเดอร์ PAID
     *  รองรับ products._id เป็นทั้ง ObjectId และ String
     *  ลองทั้งคอลเลกชัน "products" และ "product"
     * ================================================== */
    @SuppressWarnings("unchecked")
    public List<CategoryBreakdownResponse> findSalesByCategory(Instant from, Instant to) {
        String ordersCol = mongoTemplate.getCollectionName(Order.class);
        List<org.bson.Document> stages = new ArrayList<>();

        // 1) match เฉพาะออเดอร์ PAID ในช่วงเวลา
        stages.add(new org.bson.Document("$match", new org.bson.Document("$and", Arrays.asList(
                new org.bson.Document("status", "PAID"),
                new org.bson.Document("$or", Arrays.asList(
                        new org.bson.Document("paidAt",    new org.bson.Document("$gte", from).append("$lt", to)),
                        new org.bson.Document("updatedAt", new org.bson.Document("$gte", from).append("$lt", to)),
                        new org.bson.Document("createdAt", new org.bson.Document("$gte", from).append("$lt", to))
                ))
        ))));

        // 2) แตก items + ทำ pid ให้สะอาด
        stages.add(new org.bson.Document("$unwind", "$items"));
        stages.add(new org.bson.Document("$addFields",
                new org.bson.Document("pidClean",
                        new org.bson.Document("$trim", new org.bson.Document("input", "$items.productId")))
        ));

        // 3) แปลง pidClean -> ObjectId (onError/onNull = null)
        stages.add(new org.bson.Document("$addFields",
                new org.bson.Document("pidObj",
                        new org.bson.Document("$convert", new org.bson.Document()
                                .append("input", "$pidClean")
                                .append("to", "objectId")
                                .append("onError", null)
                                .append("onNull",  null)))));

        // 4) lookup ไป products: ให้เทียบได้ทั้ง ObjectId และ String และ toString(_id)
        stages.add(new org.bson.Document("$lookup", new org.bson.Document()
                .append("from", "products")
                .append("let", new org.bson.Document("pidClean", "$pidClean").append("pidObj", "$pidObj"))
                .append("pipeline", Arrays.asList(new org.bson.Document("$match",
                        new org.bson.Document("$expr",
                                new org.bson.Document("$or", Arrays.asList(
                                        new org.bson.Document("$eq", Arrays.asList("$_id", "$$pidObj")),
                                        new org.bson.Document("$eq", Arrays.asList("$_id", "$$pidClean")),
                                        new org.bson.Document("$eq", Arrays.asList(
                                                new org.bson.Document("$toString", "$_id"),
                                                "$$pidClean"))
                                ))
                        )
                )))
                .append("as", "p")
        ));

        // 5) ดึงชื่อหมวด (category / categoryName / category.name), ถ้าไม่มี -> "Uncategorized"
        stages.add(new org.bson.Document("$addFields", new org.bson.Document("cat",
                new org.bson.Document("$let", new org.bson.Document()
                        .append("vars", new org.bson.Document("doc",
                                new org.bson.Document("$cond", Arrays.asList(
                                        new org.bson.Document("$gt", Arrays.asList(new org.bson.Document("$size", "$p"), 0)),
                                        new org.bson.Document("$arrayElemAt", Arrays.asList("$p", 0)),
                                        null   // << ใช้ Arrays.asList เพื่อให้ใส่ null ได้
                                ))
                        ))
                        .append("in", new org.bson.Document("$ifNull", Arrays.asList(
                                new org.bson.Document("$ifNull", Arrays.asList(
                                        "$$doc.category",
                                        new org.bson.Document("$ifNull", Arrays.asList(
                                                "$$doc.categoryName",
                                                new org.bson.Document("$getField",
                                                        new org.bson.Document("field", "name")
                                                                .append("input", "$$doc.category"))
                                        ))
                                )),
                                "Uncategorized"
                        )))
                ))
        ));

        // 6) รวม qty ต่อ category
        stages.add(new org.bson.Document("$group",
                new org.bson.Document("_id", "$cat")
                        .append("value", new org.bson.Document("$sum", "$items.quantity"))
        ));

        // 7) project + sort
        stages.add(new org.bson.Document("$project",
                new org.bson.Document("_id", 0).append("category", "$_id").append("value", 1)
        ));
        stages.add(new org.bson.Document("$sort", new org.bson.Document("value", -1)));

        // run
        List<CategoryBreakdownResponse> out = new ArrayList<>();
        mongoTemplate.getDb().getCollection(ordersCol)
                .aggregate(stages)
                .forEach(d -> out.add(new CategoryBreakdownResponse(
                        String.valueOf(d.get("category")),
                        ((Number) d.get("value")).longValue()
                )));
        return out;
    }

    /* ==========================================
     *   SUMMARY: Revenue / Orders / AOV (เดิม)
     * ========================================== */
    public DashboardSummaryResponse findSummary(Instant from, Instant to) {
        String collection = mongoTemplate.getCollectionName(Order.class);

        Criteria cPaid = Criteria.where("status").is("PAID");
        Criteria cTime = new Criteria().orOperator(
                Criteria.where("paidAt").gte(from).lt(to),
                Criteria.where("updatedAt").gte(from).lt(to),
                Criteria.where("createdAt").gte(from).lt(to)
        );

        Query q = new Query(cPaid).addCriteria(cTime);
        q.fields()
                .include("subTotal")
                .include("total")
                .include("items")
                .include("paidAt").include("updatedAt").include("createdAt");

        List<Document> orders = mongoTemplate.find(q, Document.class, collection);

        long orderCount = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        for (Document od : orders) {
            orderCount++;

            BigDecimal v = toBigDecimal(od.get("subTotal"));
            if (v.compareTo(BigDecimal.ZERO) == 0) {
                List<Document> items = (List<Document>) od.get("items");
                if (items != null) {
                    for (Document it : items) {
                        long qty = toLong(it.get("quantity"));
                        BigDecimal price = toBigDecimal(it.get("unitPrice"));
                        v = v.add(price.multiply(BigDecimal.valueOf(qty)));
                    }
                }
            }
            if (v.compareTo(BigDecimal.ZERO) == 0) {
                v = toBigDecimal(od.get("total"));
            }
            revenue = revenue.add(v);
        }

        BigDecimal aov = orderCount == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(orderCount), 2, java.math.RoundingMode.HALF_UP);

        return DashboardSummaryResponse.builder()
                .revenue(revenue)
                .orders(orderCount)
                .averageOrderValue(aov)
                .build();
    }

    @SuppressWarnings("unchecked")
    public List<RevenuePointResponse> revenueDaily(Instant from, Instant to, ZoneId tz) {
        String collection = mongoTemplate.getCollectionName(Order.class);

        Criteria cPaid = Criteria.where("status").is("PAID");
        Criteria cTime = new Criteria().orOperator(
                Criteria.where("paidAt").gte(from).lt(to),
                Criteria.where("updatedAt").gte(from).lt(to),
                Criteria.where("createdAt").gte(from).lt(to)
        );

        Query q = new Query(cPaid).addCriteria(cTime);
        q.fields()
                .include("subTotal").include("total").include("items")
                .include("paidAt").include("updatedAt").include("createdAt");

        List<Document> orders = mongoTemplate.find(q, Document.class, collection);

        // รวมยอดต่อ LocalDate (ตามโซนเวลา)
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (Document od : orders) {
            Instant basis =
                    od.get("paidAt", Instant.class) != null ? od.get("paidAt", Instant.class) :
                            od.get("updatedAt", Instant.class) != null ? od.get("updatedAt", Instant.class) :
                                    od.get("createdAt", Instant.class);

            if (basis == null) continue;

            LocalDate day = LocalDateTime.ofInstant(basis, tz).toLocalDate();

            BigDecimal v = toBigDecimal(od.get("subTotal"));
            if (v.compareTo(BigDecimal.ZERO) == 0) {
                List<Document> items = (List<Document>) od.get("items");
                if (items != null) {
                    for (Document it : items) {
                        long qty = toLong(it.get("quantity"));
                        BigDecimal price = toBigDecimal(it.get("unitPrice"));
                        v = v.add(price.multiply(BigDecimal.valueOf(qty)));
                    }
                }
            }
            if (v.compareTo(BigDecimal.ZERO) == 0) {
                v = toBigDecimal(od.get("total"));
            }

            byDay.merge(day, v, BigDecimal::add);
        }

        // Zero-fill ให้ทุกวันในช่วงมีค่า (เพื่อให้กราฟต่อเนื่อง)
        List<RevenuePointResponse> out = new ArrayList<>();
        LocalDate start = LocalDateTime.ofInstant(from, tz).toLocalDate();
        LocalDate end   = LocalDateTime.ofInstant(to, tz).toLocalDate();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            out.add(RevenuePointResponse.builder()
                    .date(d)
                    .revenue(byDay.getOrDefault(d, BigDecimal.ZERO))
                    .build());
        }

        // ตัดวันสุดท้ายทิ้งถ้า to ชี้ไป “กลางวัน” แล้วคุณอยากรวมเฉพาะถึงวันนี้ (เลือกได้)
        return out;
    }

    /* ---------- helpers ---------- */
    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v);
        return "null".equalsIgnoreCase(s) ? null : s;
    }
    private static long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0L; }
    }
    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof org.bson.types.Decimal128 d128) return d128.bigDecimalValue();
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
    private static class Acc {
        String name;
        long units = 0;
        BigDecimal revenue = BigDecimal.ZERO;
    }
}
