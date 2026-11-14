package com.university.ums.dto;

import com.university.ums.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank
        @Email
        @Size(max = 160)
        String email,
        @Size(min = 6, max = 100)
        String password,
        @NotBlank
        @Size(max = 120)
        String fullName,
        @NotNull
        Role role,
        @NotNull
        Boolean enabled
) {
}
