package com.university.ums.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9]{2,12}$", message = "Use 2 to 12 letters or numbers")
        String code,
        @NotBlank
        @Size(max = 160)
        String title,
        @Size(max = 1000)
        String description,
        @NotNull
        @Min(1)
        @Max(12)
        Integer credits,
        @NotNull
        Long departmentId,
        Long teacherId
) {
}
