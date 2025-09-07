package com.shirtshop.config;

import com.shirtshop.security.CustomOAuth2UserService;
import com.shirtshop.security.JwtAuthenticationFilter;
import com.shirtshop.security.JwtService;
import com.shirtshop.security.OAuth2SuccessHandler;
import com.shirtshop.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);

        http.cors(cors -> cors.configurationSource(req -> {
            var cfg = new CorsConfiguration();
            cfg.setAllowedOrigins(List.of("http://localhost:3000","http://localhost:5173"));
            cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
            cfg.setAllowedHeaders(List.of("*"));
            cfg.setAllowCredentials(true);
            return cfg;
        }));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/swagger-ui/**","/v3/api-docs/**",
                        "/api/auth/register","/api/auth/login","/api/auth/refresh",
                        "/api/auth/forgot-password","/api/auth/reset-password",
                        "/api/media/**",
                        "/oauth2/**","/login/oauth2/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/me/**").authenticated()
                .anyRequest().authenticated()
        );

        http.oauth2Login(oauth -> oauth
                .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
        );

        http.addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository),
                AnonymousAuthenticationFilter.class);

        return http.build();
    }
}
