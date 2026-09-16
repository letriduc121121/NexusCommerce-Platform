package com.ecommerce.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String status;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
