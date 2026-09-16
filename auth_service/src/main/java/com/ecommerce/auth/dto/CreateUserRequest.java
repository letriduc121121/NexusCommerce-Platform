package com.ecommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;

    @Size(max = 100)
    private String fullName;

    @Pattern(regexp = "^$|^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Builder.Default
    private String status = "ACTIVE";

    @NotBlank(message = "Vai trò (role) không được để trống")
    @Pattern(regexp = "^(USER|ADMIN)$", message = "Vai trò chỉ nhận giá trị USER hoặc ADMIN")
    private String role;
}
