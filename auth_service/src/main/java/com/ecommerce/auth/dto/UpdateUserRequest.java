package com.ecommerce.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Size(max = 100)
    private String fullName;

    @Pattern(regexp = "^$|^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Pattern(regexp = "^$|^(ACTIVE|LOCKED)$", message = "Trạng thái chỉ nhận giá trị ACTIVE hoặc LOCKED")
    private String status;

    @Pattern(regexp = "^$|^(USER|ADMIN)$", message = "Vai trò chỉ nhận giá trị USER hoặc ADMIN")
    private String role;
}
