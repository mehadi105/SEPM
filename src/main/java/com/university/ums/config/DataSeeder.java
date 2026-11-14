package com.university.ums.config;

import com.university.ums.domain.Course;
import com.university.ums.domain.Department;
import com.university.ums.domain.Enrollment;
import com.university.ums.domain.EnrollmentStatus;
import com.university.ums.domain.Role;
import com.university.ums.domain.User;
import com.university.ums.repo.CourseRepository;
import com.university.ums.repo.DepartmentRepository;
import com.university.ums.repo.EnrollmentRepository;
import com.university.ums.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository users;
    private final DepartmentRepository departments;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void seed() {
        if (users.count() > 0) {
            log.info("Database already has users; seed data was left unchanged");
            return;
        }

        Department cs = department("Computer Science", "CS");
        Department math = department("Mathematics", "MATH");
        Department bus = department("Business Administration", "BUS");

        User admin = user("admin", "alex.morgan@harborview.edu", "admin123", "Alex Morgan", Role.ADMIN);
        User sarah = user("teacher", "sarah.khan@harborview.edu", "teacher123", "Dr. Sarah Khan", Role.TEACHER);
        User james = user("teacher2", "james.okonkwo@harborview.edu", "teacher123", "Prof. James Okonkwo", Role.TEACHER);
        User ayesha = user("student", "ayesha.rahman@harborview.edu", "student123", "Ayesha Rahman", Role.STUDENT);
        User liam = user("student2", "liam.chen@harborview.edu", "student123", "Liam Chen", Role.STUDENT);
        User maya = user("student3", "maya.iqbal@harborview.edu", "student123", "Maya Iqbal", Role.STUDENT);

        Course cs101 = course(
                "CS101",
                "Introduction to Programming",
                "Variables, control flow, methods, and a first look at objects in Java.",
                3,
                cs,
                sarah);
        course(
                "CS201",
                "Data Structures",
                "Lists, stacks, queues, trees, and hash tables.",
                3,
                cs,
                sarah);
        Course math101 = course(
                "MATH101",
                "Calculus I",
                "Limits, derivatives, and the start of integration.",
                4,
                math,
                james);
        course(
                "BUS110",
                "Principles of Management",
                "Planning, organizing, leading, and controlling in organizations.",
                3,
                bus,
                null);

        enroll(ayesha, cs101, LocalDateTime.now().minusDays(12));
        enroll(liam, math101, LocalDateTime.now().minusDays(5));

        log.info("Seeded Harborview University demo data (admin/admin123, teacher/teacher123, student/student123)");
    }

    private Department department(String name, String code) {
        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        return departments.save(department);
    }

    private User user(String username, String email, String password, String fullName, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setEnabled(true);
        return users.save(user);
    }

    private Course course(String code, String title, String description, int credits, Department department, User teacher) {
        Course course = new Course();
        course.setCode(code);
        course.setTitle(title);
        course.setDescription(description);
        course.setCredits(credits);
        course.setDepartment(department);
        course.setTeacher(teacher);
        return courses.save(course);
    }

    private void enroll(User student, Course course, LocalDateTime when) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrolledAt(when);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollments.save(enrollment);
    }
}
