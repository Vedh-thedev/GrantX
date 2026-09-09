package com.grantx.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private String tokenType = "Bearer";
        private Long userId;
        private String username;
        private String fullName;
        private String role;
        private String email;

        public LoginResponse(String token, Long userId, String username, String fullName, String role, String email) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.email = email;
        }
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Role is required")
        private String role;

        private String phone;

        // Student fields
        private String registerNumber;
        private String department;
        private Integer yearOfStudy;

        // Faculty fields
        private String employeeId;
        private String designation;
        private String specialization;
    }
}
