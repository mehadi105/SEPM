package com.university.ums.dto;

import com.university.ums.domain.Role;

public record UserSummary(
        Long id,
        String username,
        String fullName,
        Role role
) {
}
