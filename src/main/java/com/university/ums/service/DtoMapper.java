package com.university.ums.service;

import com.university.ums.domain.Course;
import com.university.ums.domain.Department;
import com.university.ums.domain.Enrollment;
import com.university.ums.domain.User;
import com.university.ums.dto.CourseResponse;
import com.university.ums.dto.DepartmentResponse;
import com.university.ums.dto.EnrollmentResponse;
import com.university.ums.dto.UserResponse;
import com.university.ums.dto.UserSummary;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public UserResponse toUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled());
    }

    public UserSummary toSummary(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummary(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
    }

    public DepartmentResponse toDepartment(Department department) {
        return new DepartmentResponse(department.getId(), department.getName(), department.getCode());
    }

    public CourseResponse toCourse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                course.getDescription(),
                course.getCredits(),
                toDepartment(course.getDepartment()),
                toSummary(course.getTeacher()));
    }

    public EnrollmentResponse toEnrollment(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                toSummary(enrollment.getStudent()),
                toCourse(enrollment.getCourse()),
                enrollment.getEnrolledAt(),
                enrollment.getGrade(),
                enrollment.getStatus());
    }
}
