package com.university.ums.dto;

import com.university.ums.domain.Role;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Role role,
        boolean enabled
) {
}
