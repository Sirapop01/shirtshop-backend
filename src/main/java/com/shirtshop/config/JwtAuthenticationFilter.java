// src/main/java/com/shirtshop/config/JwtAuthenticationFilter.java
package com.shirtshop.config;

import com.shirtshop.entity.User;
import com.shirtshop.repository.UserRepository;
import com.shirtshop.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {


        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        // token ไม่ valid → ปล่อยผ่าน (อย่าฟันธง 401/403 ตรงนี้)
        if (!jwtService.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ดึง userId จาก token
        String userId;
        try {
            userId = jwtService.extractUserId(token);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        // ตั้ง Authentication ใน SecurityContext ถ้ายังไม่ได้ตั้ง
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = userService.findByIdOrThrow(userId);

            List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                    .toList();

            // ใช้ userId เป็น principal (authentication.getName() จะคืน userId)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user.getId(), null, authorities);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // ให้ request ผ่านต่อ
        filterChain.doFilter(request, response);
    }
}
