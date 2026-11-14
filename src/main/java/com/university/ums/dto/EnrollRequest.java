package com.university.ums.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollRequest(
        @NotNull Long courseId,
        Long studentId
) {
}
