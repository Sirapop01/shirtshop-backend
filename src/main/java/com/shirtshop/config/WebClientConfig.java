package com.shirtshop.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    @Value("${ml.gradio.base-url}")
    private String baseUrl;

    @Value("${ml.gradio.api-key:}")
    private String apiKey;

    @Bean("mlWebClient")
    public WebClient mlWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                // ⬇️ ยืดเวลา response (เช่น 180 วินาที)
                .responseTimeout(Duration.ofSeconds(180))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(180, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(180, TimeUnit.SECONDS));
                });

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // ⬇️ ขยายบัฟเฟอร์รับ body (เช่น 64MB)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(64 * 1024 * 1024))
                        .build())
                .defaultHeaders(h -> {
                    if (apiKey != null && !apiKey.isBlank()) {
                        h.set("Authorization", "Bearer " + apiKey);
                    }
                })
                .build();
    }
}
