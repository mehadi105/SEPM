package com.university.ums.unit;

import com.university.ums.domain.Course;
import com.university.ums.domain.Department;
import com.university.ums.domain.Enrollment;
import com.university.ums.domain.EnrollmentStatus;
import com.university.ums.domain.Role;
import com.university.ums.domain.User;
import com.university.ums.dto.EnrollmentResponse;
import com.university.ums.dto.GradeRequest;
import com.university.ums.exception.ApiException;
import com.university.ums.repo.CourseRepository;
import com.university.ums.repo.EnrollmentRepository;
import com.university.ums.repo.UserRepository;
import com.university.ums.service.CurrentUserService;
import com.university.ums.service.DtoMapper;
import com.university.ums.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserService currentUser;

    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(
                enrollmentRepository,
                courseRepository,
                userRepository,
                currentUser,
                new DtoMapper());
    }

    @Test
    void gradeMarksEnrollmentCompletedWhenTeacherOwnsCourse() {
        User teacher = user(10L, "teacher", Role.TEACHER);
        User student = user(20L, "student", Role.STUDENT);
        Course course = course(5L, "CS101", teacher);
        Enrollment enrollment = enrollment(3L, student, course);

        when(currentUser.require()).thenReturn(teacher);
        when(enrollmentRepository.findWithDetailsById(3L)).thenReturn(Optional.of(enrollment));

        EnrollmentResponse response = enrollmentService.grade(3L, new GradeRequest("a"));

        assertEquals("A", response.grade());
        assertEquals(EnrollmentStatus.COMPLETED, response.status());
        assertEquals(EnrollmentStatus.COMPLETED, enrollment.getStatus());
    }

    @Test
    void gradeRejectsInvalidLetter() {
        User admin = user(1L, "admin", Role.ADMIN);
        Enrollment enrollment = enrollment(3L, user(20L, "student", Role.STUDENT), course(5L, "CS101", null));

        when(currentUser.require()).thenReturn(admin);
        when(enrollmentRepository.findWithDetailsById(3L)).thenReturn(Optional.of(enrollment));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> enrollmentService.grade(3L, new GradeRequest("Z")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Grade must be A, A-, B+, B, B-, C+, C, C-, D, or F", ex.getMessage());
    }

    private static User user(Long id, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@uni.edu");
        user.setFullName(username);
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private static Course course(Long id, String code, User teacher) {
        Department department = new Department();
        department.setId(1L);
        department.setCode("CSE");
        department.setName("Computer Science");

        Course course = new Course();
        course.setId(id);
        course.setCode(code);
        course.setTitle(code + " Title");
        course.setCredits(3);
        course.setDepartment(department);
        course.setTeacher(teacher);
        return course;
    }

    private static Enrollment enrollment(Long id, User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(id);
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        return enrollment;
    }
}
