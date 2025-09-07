package com.shirtshop.security;

import com.shirtshop.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response); // ไม่มี token → ปล่อยผ่าน (สำคัญ)
            return;
        }

        String token = header.substring(7);
        try {
            var claims = jwtService.parseAccess(token);      // ตรวจลายเซ็น/หมดอายุ
            String userId = claims.getSubject();             // เราเก็บ userId ไว้ที่ sub
            var roles = jwtService.extractRoles(claims);     // ["USER", "ADMIN", ...]

            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    roles.stream().map(SimpleGrantedAuthority::new).toList()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException ex) {
            // Token ไม่ผ่าน → แค่ไม่ตั้ง auth แล้วปล่อยผ่าน (อย่าตอบ 401/403 ตรงนี้)
        }

        chain.doFilter(request, response);
    }
}
