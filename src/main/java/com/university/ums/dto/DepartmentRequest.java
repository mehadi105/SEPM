package com.university.ums.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank
        @Size(max = 120)
        String name,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9]{2,10}$", message = "Use 2 to 10 letters or numbers")
        String code
) {
}
