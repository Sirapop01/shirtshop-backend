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
        var body = new java.util.HashMap<String, Object>();
        body.put("from", from);                    // "StyleWhere <onboarding@resend.dev>"
        body.put("to", new String[]{to});
        body.put("subject", subject);
        body.put("html", html);
        body.put("reply_to", System.getenv().getOrDefault("APP_MAIL_REPLY_TO", "stylewhere68@gmail.com"));
        client.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
