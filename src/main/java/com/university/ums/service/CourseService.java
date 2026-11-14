package com.university.ums.service;

import com.university.ums.domain.Course;
import com.university.ums.domain.Department;
import com.university.ums.domain.Role;
import com.university.ums.domain.User;
import com.university.ums.dto.CourseRequest;
import com.university.ums.dto.CourseResponse;
import com.university.ums.exception.ApiException;
import com.university.ums.repo.CourseRepository;
import com.university.ums.repo.DepartmentRepository;
import com.university.ums.repo.EnrollmentRepository;
import com.university.ums.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUser;
    private final DtoMapper mapper;

    public List<CourseResponse> list() {
        return courseRepository.findAllWithDetails().stream().map(mapper::toCourse).toList();
    }

    public List<CourseResponse> teaching() {
        User actor = currentUser.require();
        if (actor.getRole() != Role.TEACHER) {
            throw ApiException.forbidden("Only teachers have a teaching list");
        }
        return courseRepository.findByTeacherIdWithDetails(actor.getId()).stream().map(mapper::toCourse).toList();
    }

    public CourseResponse get(Long id) {
        return mapper.toCourse(require(id));
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        String code = normalizeCode(request.code());
        if (courseRepository.findByCode(code).isPresent()) {
            throw ApiException.conflict("Course code is already in use");
        }
        Course course = new Course();
        apply(course, request, code);
        return mapper.toCourse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = require(id);
        String code = normalizeCode(request.code());
        courseRepository.findByCode(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw ApiException.conflict("Course code is already in use");
                });
        apply(course, request, code);
        return mapper.toCourse(course);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        enrollmentRepository.deleteByCourseId(id);
        courseRepository.deleteById(id);
    }

    private void apply(Course course, CourseRequest request, String code) {
        course.setCode(code);
        course.setTitle(request.title().trim());
        course.setDescription(clean(request.description()));
        course.setCredits(request.credits());
        course.setDepartment(requireDepartment(request.departmentId()));
        course.setTeacher(resolveTeacher(request.teacherId()));
    }

    private Course require(Long id) {
        return courseRepository.findWithDetailsById(id).orElseThrow(() -> ApiException.notFound("Course not found"));
    }

    private Department requireDepartment(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Department not found"));
    }

    private User resolveTeacher(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        User teacher = userRepository.findById(teacherId).orElseThrow(() -> ApiException.notFound("Teacher not found"));
        if (teacher.getRole() != Role.TEACHER) {
            throw ApiException.badRequest("Assigned user must have the teacher role");
        }
        if (!teacher.isEnabled()) {
            throw ApiException.badRequest("Assigned teacher account is disabled");
        }
        return teacher;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
