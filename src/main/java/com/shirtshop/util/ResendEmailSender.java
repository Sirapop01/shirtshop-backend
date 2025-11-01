package com.shirtshop.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * ส่งอีเมลผ่าน Resend HTTP API
 * เปิดใช้ด้วยโปรไฟล์: 'resend'
 */
@Service
@org.springframework.context.annotation.Profile("resend")
public class ResendEmailSender implements EmailSender {

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
    }

    @Override
    public void send(String to, String subject, String html) {
        var body = Map.of(
                "from", from,
                "to", new String[]{to},
                "subject", subject,
                "html", html
        );

        client.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
