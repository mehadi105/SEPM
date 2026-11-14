package com.university.ums.dto;

import com.university.ums.domain.EnrollmentStatus;

import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long id,
        UserSummary student,
        CourseResponse course,
        LocalDateTime enrolledAt,
        String grade,
        EnrollmentStatus status
) {
}
