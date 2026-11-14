package com.university.ums.web;

import com.university.ums.dto.EnrollRequest;
import com.university.ums.dto.EnrollmentResponse;
import com.university.ums.dto.GradeRequest;
import com.university.ums.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @Operation(summary = "List enrollments visible to the signed-in user")
    public List<EnrollmentResponse> list(@RequestParam(required = false) Long courseId) {
        return enrollmentService.list(courseId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one enrollment")
    public EnrollmentResponse get(@PathVariable Long id) {
        return enrollmentService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @Operation(summary = "Enroll a student. Students enroll themselves; admins pass studentId.")
    public ResponseEntity<EnrollmentResponse> enroll(@Valid @RequestBody EnrollRequest request) {
        EnrollmentResponse body = enrollmentService.enroll(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(body.id()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @ResponseStatus(NO_CONTENT)
    @Operation(summary = "Drop an enrollment. Students can drop only their own ungraded enrollment.")
    public void drop(@PathVariable Long id) {
        enrollmentService.drop(id);
    }

    @PatchMapping("/{id}/grade")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Record or clear a grade. Teachers can grade only their own courses.")
    public EnrollmentResponse grade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return enrollmentService.grade(id, request);
    }
}
