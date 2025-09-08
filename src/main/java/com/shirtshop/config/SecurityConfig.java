package com.shirtshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            OAuth2LoginSuccessHandler successHandler,
                                            OAuth2LoginFailureHandler failureHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // REST API
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // อนุญาตทุกเมธอดใน /api/auth/**
                        .requestMatchers("/api/auth/**").permitAll()
                        // (ถ้าอยากชัดเจนก็เก็บรายการ POST ทีละอันไว้ได้เหมือนเดิม)
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/oauth2/authorization/**").permitAll() // เผื่อ preflight
                        .anyRequest().authenticated()
                )
                .oauth2Login(o -> o
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                );

        // ถ้ายังไม่ใช้ OAuth2 login ตอนนี้ ไม่ต้องเปิดเลย (เอา dependency ออกได้ยิ่งดี)
        // ไม่ต้องเรียก http.oauth2Login(...) หากยังไม่ใช้

        return http.build();
    }
}
