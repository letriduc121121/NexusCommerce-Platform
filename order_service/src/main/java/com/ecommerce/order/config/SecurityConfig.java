package com.ecommerce.order.config;

import com.ecommerce.order.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // SockJS bắt tay bằng nhiều request phụ không kèm header Authorization.
                .requestMatchers("/ws-orders/**", "/error").permitAll()
                // Xem toàn bộ đơn và đổi trạng thái là quyền riêng của Admin.
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/admin").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/orders/admin/approve-all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*/status").hasRole("ADMIN")
                .requestMatchers("/api/v1/orders/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
