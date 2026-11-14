package com.university.ums.dto;

import com.university.ums.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$", message = "Use letters, numbers, dots, underscores, or hyphens")
        String username,
        @NotBlank
        @Email
        @Size(max = 160)
        String email,
        @NotBlank
        @Size(min = 6, max = 100)
        String password,
        @NotBlank
        @Size(max = 120)
        String fullName,
        @NotNull
        Role role
) {
}
