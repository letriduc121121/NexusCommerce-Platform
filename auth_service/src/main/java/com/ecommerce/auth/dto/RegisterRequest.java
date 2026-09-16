package com.ecommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Họ và tên không được để trống!")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự!")
    private String fullName;

    @Pattern(regexp = "^$|^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ!")
    private String phone;

    @NotBlank(message = "Email không được để trống!")
    @Email(message = "Email không đúng định dạng!")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống!")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự!")
    private String password;
}
