// src/main/java/com/shirtshop/util/ResendEmailSender.java
package com.shirtshop.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@org.springframework.context.annotation.Profile("resend")
public class ResendEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

    private final RestClient client;
    private final String from;

    public ResendEmailSender(
            @Value("${resend.apiKey}") String apiKey,
            @Value("${app.mail.from}") String from
    ) {
        this.client = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.from = from;
        log.info("ResendEmailSender init with from='{}'", from);
    }

    @Override
    public void send(String to, String subject, String html) {
        var body = Map.of(
                "from", from,
                "to", new String[]{to},
                "subject", subject,
                "html", html
        );
        log.info("Sending email via Resend → to='{}' subject='{}'", to, subject);
        try {
            client.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Resend API call: OK (2xx)");
        } catch (Exception e) {
            log.error("Resend API call FAILED", e);
            throw e; // ให้เห็น error ชัด ถ้าอยากไม่ล้มค่อยจับที่ service ชั้นบน
        }
    }
}
