// src/main/java/com/shirtshop/config/SecurityConfig.java
package com.shirtshop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor    
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ---------- Public ----------
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/media/upload").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()

                        // ---------- Public (Mock Hosted Payment pages) ----------
                        .requestMatchers(
                                "/mock-hosted-checkout",
                                "/mock-hosted-pay",
                                "/mock-hosted-cancel"
                        ).permitAll()

                        // ---------- Admin only ----------
                        .requestMatchers(HttpMethod.POST,   "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // ---------- User protected ----------
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/cart/**").authenticated()     // ✅ เพิ่มตรงนี้
                        .requestMatchers("/api/orders/**").authenticated()   // ✅ เตรียมไว้สำหรับ order
                        .requestMatchers("/api/addresses/**").authenticated()
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/api/products/**").authenticated()
                        .requestMatchers("/api/payments/**").authenticated()
                        // ---------- Others ----------
                        .anyRequest().authenticated()

                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // CORS ให้ FE localhost:3000 เรียกได้
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:3000"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
