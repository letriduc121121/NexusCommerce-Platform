package com.ecommerce.inventory.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String jwt = resolveToken(request);
            if (StringUtils.hasText(jwt)) {
                if (tokenProvider.validateToken(jwt)) {
                    Claims claims = tokenProvider.getClaimsFromToken(jwt);
                    List<SimpleGrantedAuthority> authorities = Arrays.stream(claims.get("roles", String.class).split(","))
                            .filter(role -> !role.isBlank())
                            .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                            .collect(Collectors.toList());

                    var authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("Token không hợp lệ hoặc đã hết hạn: {} {}", request.getMethod(), request.getRequestURI());
                }
            }
        } catch (Exception ex) {
            log.warn("Không thể xác thực token cho {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
    }
}
