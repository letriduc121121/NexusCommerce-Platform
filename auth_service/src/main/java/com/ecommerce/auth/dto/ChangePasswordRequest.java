package com.ecommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Mật khẩu cũ không được để trống!")
    private String oldPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống!")
    @Pattern(
        regexp = "^[A-Z](?=.*[0-9])(?=.*[@#$%^&+=!._-]).{5,}$",
        message = "Mật khẩu mới phải bắt đầu bằng chữ cái viết hoa, chứa ít nhất một chữ số và ít nhất một ký tự đặc biệt!"
    )
    private String newPassword;
}
