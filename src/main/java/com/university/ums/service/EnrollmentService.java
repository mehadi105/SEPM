package com.university.ums.service;

import com.university.ums.domain.Course;
import com.university.ums.domain.Enrollment;
import com.university.ums.domain.EnrollmentStatus;
import com.university.ums.domain.Role;
import com.university.ums.domain.User;
import com.university.ums.dto.EnrollRequest;
import com.university.ums.dto.EnrollmentResponse;
import com.university.ums.dto.GradeRequest;
import com.university.ums.exception.ApiException;
import com.university.ums.repo.CourseRepository;
import com.university.ums.repo.EnrollmentRepository;
import com.university.ums.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private static final Pattern GRADE = Pattern.compile("^(A|A-|B\\+|B|B-|C\\+|C|C-|D|F)$");

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUser;
    private final DtoMapper mapper;

    public List<EnrollmentResponse> list(Long courseId) {
        User actor = currentUser.require();
        if (courseId != null && !courseRepository.existsById(courseId)) {
            throw ApiException.notFound("Course not found");
        }
        List<Enrollment> enrollments = switch (actor.getRole()) {
            case ADMIN -> enrollmentRepository.findAllWithDetails();
            case TEACHER -> {
                if (courseId != null) {
                    assertTeacherOwnsCourse(actor, courseId);
                }
                yield enrollmentRepository.findByTeacherIdWithDetails(actor.getId());
            }
            case STUDENT -> enrollmentRepository.findByStudentIdWithDetails(actor.getId());
        };
        return enrollments.stream()
                .filter(enrollment -> courseId == null || enrollment.getCourse().getId().equals(courseId))
                .map(mapper::toEnrollment)
                .toList();
    }

    public EnrollmentResponse get(Long id) {
        User actor = currentUser.require();
        Enrollment enrollment = require(id);
        assertCanView(enrollment, actor);
        return mapper.toEnrollment(enrollment);
    }

    @Transactional
    public EnrollmentResponse enroll(EnrollRequest request) {
        User actor = currentUser.require();
        User student = resolveStudent(actor, request.studentId());
        Course course = courseRepository.findWithDetailsById(request.courseId())
                .orElseThrow(() -> ApiException.notFound("Course not found"));
        if (enrollmentRepository.findByStudentIdAndCourseId(student.getId(), course.getId()).isPresent()) {
            throw ApiException.conflict("Already enrolled in this course");
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        return mapper.toEnrollment(enrollmentRepository.save(enrollment));
    }

    @Transactional
    public void drop(Long id) {
        User actor = currentUser.require();
        Enrollment enrollment = require(id);
        if (actor.getRole() == Role.STUDENT) {
            if (!enrollment.getStudent().getId().equals(actor.getId())) {
                throw ApiException.forbidden("You can only drop your own enrollment");
            }
            if (enrollment.getGrade() != null || enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
                throw ApiException.conflict("A graded enrollment cannot be dropped");
            }
        } else if (actor.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("You do not have permission to drop this enrollment");
        }
        enrollmentRepository.delete(enrollment);
    }

    @Transactional
    public EnrollmentResponse grade(Long id, GradeRequest request) {
        User actor = currentUser.require();
        Enrollment enrollment = require(id);
        assertCanGrade(enrollment, actor);
        String normalized = request.grade() == null ? "" : request.grade().trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            enrollment.setGrade(null);
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
        } else if (!GRADE.matcher(normalized).matches()) {
            throw ApiException.badRequest("Grade must be A, A-, B+, B, B-, C+, C, C-, D, or F");
        } else {
            enrollment.setGrade(normalized);
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
        }
        return mapper.toEnrollment(enrollment);
    }

    private User resolveStudent(User actor, Long studentId) {
        if (actor.getRole() == Role.STUDENT) {
            return actor;
        }
        if (actor.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Teachers cannot enroll students");
        }
        if (studentId == null) {
            throw ApiException.badRequest("studentId is required when an admin enrolls someone");
        }
        User student = userRepository.findById(studentId).orElseThrow(() -> ApiException.notFound("Student not found"));
        if (student.getRole() != Role.STUDENT) {
            throw ApiException.badRequest("Only students can be enrolled in a course");
        }
        if (!student.isEnabled()) {
            throw ApiException.badRequest("Student account is disabled");
        }
        return student;
    }

    private void assertTeacherOwnsCourse(User actor, Long courseId) {
        Course course = courseRepository.findWithDetailsById(courseId)
                .orElseThrow(() -> ApiException.notFound("Course not found"));
        if (course.getTeacher() == null || !course.getTeacher().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You can only view enrollments for your own courses");
        }
    }

    private void assertCanView(Enrollment enrollment, User actor) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (actor.getRole() == Role.STUDENT && enrollment.getStudent().getId().equals(actor.getId())) {
            return;
        }
        User teacher = enrollment.getCourse().getTeacher();
        if (actor.getRole() == Role.TEACHER && teacher != null && teacher.getId().equals(actor.getId())) {
            return;
        }
        throw ApiException.forbidden("You do not have permission to view this enrollment");
    }

    private void assertCanGrade(Enrollment enrollment, User actor) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        User teacher = enrollment.getCourse().getTeacher();
        if (actor.getRole() == Role.TEACHER && teacher != null && teacher.getId().equals(actor.getId())) {
            return;
        }
        throw ApiException.forbidden("You can only record grades for your own courses");
    }

    private Enrollment require(Long id) {
        return enrollmentRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Enrollment not found"));
    }
}
