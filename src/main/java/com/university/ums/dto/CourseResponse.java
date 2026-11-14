package com.university.ums.dto;

public record CourseResponse(
        Long id,
        String code,
        String title,
        String description,
        int credits,
        DepartmentResponse department,
        UserSummary teacher
) {
}
