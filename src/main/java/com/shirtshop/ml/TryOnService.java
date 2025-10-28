package com.shirtshop.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shirtshop.ml.dto.TryOnRequest;
import com.shirtshop.ml.dto.TryOnResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TryOnService {
    private final MLClient mlClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<TryOnResponse> tryOn(TryOnRequest req) {
        long start = System.currentTimeMillis();

        return mlClient.send(req.getPersonImageBase64(), req.getGarmentImageBase64(), req.getOptionsJson())
                .flatMap(raw -> {
                    try {
                        JsonNode root = objectMapper.readTree(raw);
                        String resultB64 = null;

                        if (root.isTextual()) {
                            // กรณีฝั่ง ML ส่ง base64 เป็นสตริงดิบ
                            resultB64 = root.asText();
                        } else if (root.has("resultBase64")) {
                            resultB64 = root.get("resultBase64").asText();
                        } else if (root.has("data")) {
                            JsonNode data = root.get("data");
                            if (data.isTextual()) {
                                resultB64 = data.asText();
                            } else if (data.isObject() && data.has("image")) {
                                resultB64 = data.get("image").asText();
                            } else if (data.isArray()) {
                                for (JsonNode item : data) {
                                    if (item.isTextual()) {
                                        resultB64 = item.asText();
                                        break;
                                    } else if (item.isObject() && item.has("image")) {
                                        resultB64 = item.get("image").asText();
                                        break;
                                    }
                                }
                            }
                        }

                        return Mono.just(TryOnResponse.builder()
                                .resultBase64(resultB64)
                                .elapsedMs(System.currentTimeMillis() - start)
                                .message(resultB64 != null ? "ok" : "no image field in response")
                                .build());
                    } catch (Exception e) {
                        // ถอด JSON ไม่ได้: อาจเป็น base64 ดิบ
                        return Mono.just(TryOnResponse.builder()
                                .resultBase64(raw)
                                .elapsedMs(System.currentTimeMillis() - start)
                                .message("parsed as raw base64")
                                .build());
                    }
                })
                // ส่งต่อสถานะ/ข้อความจาก Gradio ให้ FE เห็นตรง ๆ (ไม่เป็น 500 เงียบ ๆ)
                .onErrorResume(org.springframework.web.reactive.function.client.WebClientResponseException.class, ex -> {
                    log.error("ML HTTP {}: {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
                    return Mono.error(new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.valueOf(ex.getRawStatusCode()),
                            ex.getResponseBodyAsString()
                    ));
                })
                // เครือข่าย/timeout อื่น ๆ
                .onErrorResume(Throwable.class, ex -> {
                    log.error("ML call failed", ex);
                    return Mono.error(new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "ML upstream unavailable"));
                });
    }
}
