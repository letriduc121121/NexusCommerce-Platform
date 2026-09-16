package com.ecommerce.promotion.config;

import com.ecommerce.promotion.security.JwtAuthenticationFilter;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // Trang chủ xem mã công khai - không cần đăng nhập
                .requestMatchers(HttpMethod.GET, "/api/v1/vouchers/available").permitAll()

                // order-service gọi máy-với-máy, không có token người dùng
                .requestMatchers(HttpMethod.POST, "/api/v1/vouchers/redeem", "/api/v1/vouchers/release").permitAll()

                // Checkout - khách phải đăng nhập
                .requestMatchers(HttpMethod.POST, "/api/v1/vouchers/preview").authenticated()

                // Còn lại (CRUD + đổi trạng thái) - chỉ Admin
                .requestMatchers("/api/v1/vouchers/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
