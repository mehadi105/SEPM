package com.university.ums.dto;

import jakarta.validation.constraints.Pattern;

public record GradeRequest(
        @Pattern(
                regexp = "(?i)^(|A|A-|B\\+|B|B-|C\\+|C|C-|D|F)$",
                message = "Grade must be A, A-, B+, B, B-, C+, C, C-, D, or F"
        )
        String grade
) {
}
