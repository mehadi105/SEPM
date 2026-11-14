package com.university.ums.dto;

import com.university.ums.domain.Role;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        String email,
        String fullName,
        Role role
) {
}
