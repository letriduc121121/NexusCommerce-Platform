package com.ecommerce.inventory.config;

import com.ecommerce.inventory.security.JwtAuthenticationFilter;
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
                .requestMatchers("/error").permitAll()
                // Xem tồn kho: công khai
                .requestMatchers(HttpMethod.GET, "/api/v1/inventories/**").permitAll()
                // Reserve/release: gọi nội bộ từ Order Service, không đi qua trình duyệt của user
                .requestMatchers(HttpMethod.POST, "/api/v1/inventories/reserve", "/api/v1/inventories/release").permitAll()
                // Khởi tạo/chỉnh tồn kho: chỉ Admin
                .requestMatchers(HttpMethod.POST, "/api/v1/inventories").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/inventories/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
