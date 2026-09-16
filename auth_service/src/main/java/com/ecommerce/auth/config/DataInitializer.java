package com.ecommerce.auth.config;

import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.RoleRepository;
import com.ecommerce.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Khởi tạo vai trò (roles) trong bảng roles...");
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));

        if (userRepository.count() == 0) {
            log.info("Khởi tạo dữ liệu mẫu cho cơ sở dữ liệu users...");

            User admin = User.builder()
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("Admin123@"))
                    .status("ACTIVE")
                    .fullName("System Administrator")
                    .phone("0900000000")
                    .role(adminRole)
                    .build();

            User user1 = User.builder()
                    .email("john@example.com")
                    .password(passwordEncoder.encode("User123@"))
                    .status("ACTIVE")
                    .fullName("John Doe")
                    .phone("0901234567")
                    .role(userRole)
                    .build();

            User user2 = User.builder()
                    .email("jane@example.com")
                    .password(passwordEncoder.encode("User123@"))
                    .status("ACTIVE")
                    .fullName("Jane Smith")
                    .phone("0987654321")
                    .role(userRole)
                    .build();

            userRepository.saveAll(java.util.List.of(admin, user1, user2));
            log.info("Khởi tạo 3 tài khoản mẫu thành công!");
        } else {
            log.info("Dữ liệu bảng users đã tồn tại, bỏ qua bước khởi tạo mẫu.");
        }
    }
}
