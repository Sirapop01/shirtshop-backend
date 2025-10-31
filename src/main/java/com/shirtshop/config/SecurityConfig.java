// src/main/java/com/shirtshop/config/SecurityConfig.java
package com.shirtshop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ActiveUserFilter activeUserFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ---------- Public ----------
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()   // login/register/refresh
                        .requestMatchers(HttpMethod.GET,  "/api/products/**").permitAll() // ดูสินค้า public
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/settings/branding").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/password/otp").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/password/reset").permitAll()

                        // ---------- Admin only ----------
                        .requestMatchers(HttpMethod.POST,   "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/customers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,   "/api/admin/orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/orders/**").hasRole("ADMIN")

                        // ✅ Branding settings (ครบทั้ง 3 method ที่ใช้ multipart)
                        .requestMatchers(HttpMethod.POST,   "/api/settings/branding").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/settings/branding").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/settings/branding/**").hasRole("ADMIN")

                        // ---------- User protected ----------
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/api/addresses/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/auth/password/change").authenticated()

                        // ML try-on เปิด public
                        .requestMatchers("/api/tryon").permitAll()

                        // ---------- Others ----------
                        .anyRequest().authenticated()
                );

        // ลำดับฟิลเตอร์
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(activeUserFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS แบบ Dev/Prod-friendly
     * - ใช้ allowedOriginPatterns เพื่อรองรับ wildcard + credentials
     * - ถ้ารู้ origin แน่ชัดใน prod แนะนำล็อกโดเมนให้แคบลง
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // ✅ รองรับหลายสภาพแวดล้อม (แก้/ลบได้ตามจริงของคุณ)
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.vercel.app",
                "https://*.ngrok-free.app",
                "capacitor://localhost",
                "ionic://localhost"
        ));
        // เมธอดที่อนุญาต
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        // เฮดเดอร์ที่อนุญาต
        cfg.setAllowedHeaders(List.of(
                "Authorization","Content-Type","Accept","Origin","X-Requested-With",
                "X-CSRF-TOKEN","Cache-Control","Pragma"
        ));
        // เฮดเดอร์ที่ client มองเห็นได้ (เช่น เอา filename จาก Content-Disposition)
        cfg.setExposedHeaders(List.of(
                "Authorization","Location","Link","Content-Disposition"
        ));
        // ให้ส่งคุกกี้/Authorization ได้
        cfg.setAllowCredentials(true);
        // อายุ preflight cache (วินาที) – ลดจำนวน preflight ในเบราว์เซอร์
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
