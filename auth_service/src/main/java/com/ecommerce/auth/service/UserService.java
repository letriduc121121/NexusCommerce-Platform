package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.RoleRepository;
import com.ecommerce.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile(UUID userId) {
        log.info("👤 [USER SERVICE] Lấy thông tin cá nhân cho User ID: {}", userId);
        return mapToProfile(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        log.info("📝 [USER SERVICE] Cập nhật profile cho User ID: {}", userId);
        User user = findUser(userId);
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        return mapToProfile(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        log.info("🔑 [USER SERVICE] Đổi mật khẩu cho User ID: {}", userId);
        User user = findUser(userId);
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    public List<UserProfileResponse> getAllUsers() {
        log.info("📊 [USER SERVICE - ADMIN] Lấy danh sách toàn bộ người dùng");
        return userRepository.findAll().stream().map(this::mapToProfile).toList();
    }

    @Transactional
    public UserProfileResponse createUser(CreateUserRequest req) {
        log.info("✨ [USER SERVICE - ADMIN] Tạo người dùng mới: {}", req.getEmail());
        if (userRepository.existsByEmail(req.getEmail())) throw new RuntimeException("Email đã tồn tại!");

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .status(req.getStatus() != null ? req.getStatus() : "ACTIVE")
                .role(findOrCreateRole(req.getRole()))
                .build();

        return mapToProfile(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse updateUser(UUID id, UpdateUserRequest req) {
        log.info("📝 [USER SERVICE - ADMIN] Cập nhật người dùng ID: {}", id);
        User user = findUser(id);
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (StringUtils.hasText(req.getPhone())) user.setPhone(req.getPhone());
        if (StringUtils.hasText(req.getStatus())) user.setStatus(req.getStatus());
        if (StringUtils.hasText(req.getRole())) user.setRole(findOrCreateRole(req.getRole()));
        return mapToProfile(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID id) {
        log.info("🗑️ [USER SERVICE - ADMIN] Xóa người dùng ID: {}", id);
        User user = findUser(id);
        refreshTokenRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
    }

    private Role findOrCreateRole(String roleName) {
        String name = roleName != null ? roleName : "USER";
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
    }

    private UserProfileResponse mapToProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .status(user.getStatus())
                .role(user.getRole().getName())
                .rewardCoins(user.getRewardCoins() != null ? user.getRewardCoins() : 0L)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
