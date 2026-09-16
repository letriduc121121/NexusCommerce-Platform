package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.RoleRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        log.info("👤 [AUTH SERVICE] Đăng ký tài khoản cho email: {}", req.getEmail());

        if (userRepository.existsByEmail(req.getEmail())) throw new RuntimeException("Email đã tồn tại!");

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        User user = userRepository.save(User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .status("ACTIVE")
                .role(userRole)
                .build());

        String token = jwtTokenProvider.generateToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("✅ [AUTH SERVICE] Đăng ký tài khoản thành công! ID: {}", user.getId());
        sendKafkaEvent("auth-topic", user.getId().toString(), "ĐĂNG KÝ TÀI KHOẢN MỚI: Email=" + user.getEmail());

        return buildAuthResponse(user, token, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        log.info("🔐 [AUTH SERVICE] Đăng nhập cho email: {}", req.getEmail());

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không chính xác!"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email hoặc mật khẩu không chính xác!");
        }
        ensureActive(user);

        String token = jwtTokenProvider.generateToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("✅ [AUTH SERVICE] Đăng nhập thành công! Email: {}", user.getEmail());
        return buildAuthResponse(user, token, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest req) {
        log.info("🔄 [AUTH SERVICE] Làm mới Token...");
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(req.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ!"));

        if (tokenEntity.isRevoked() || tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token đã hết hạn hoặc bị thu hồi!");
        }

        User user = tokenEntity.getUser();
        ensureActive(user);
        String newToken = jwtTokenProvider.generateToken(user);

        log.info("✅ [AUTH SERVICE] Làm mới Token thành công!");
        return buildAuthResponse(user, newToken, tokenEntity.getToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        log.info("🚪 [AUTH SERVICE] Đăng xuất tài khoản");
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    /** Dùng chung cho login lẫn refresh - khoá tài khoản thì token cũ cũng hết quyền lấy token mới. */
    private void ensureActive(User user) {
        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa!");
        }
    }

    private AuthResponse buildAuthResponse(User user, String token, String refreshToken) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .build();
    }

    private RefreshToken createRefreshToken(User user) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build());
    }

    private void sendKafkaEvent(String topic, String key, String message) {
        try {
            kafkaTemplate.send(topic, key, message);
            log.info("📢 [KAFKA EVENT] Đã phát sự kiện lên topic '{}': {}", topic, message);
        } catch (Exception e) {
            log.error("⚠️ [KAFKA ERROR] Lỗi phát sự kiện Kafka: {}", e.getMessage());
        }
    }
}
