package com.university.ums.service;

import com.university.ums.domain.Role;
import com.university.ums.domain.User;
import com.university.ums.dto.UserCreateRequest;
import com.university.ums.dto.UserResponse;
import com.university.ums.dto.UserUpdateRequest;
import com.university.ums.exception.ApiException;
import com.university.ums.repo.CourseRepository;
import com.university.ums.repo.EnrollmentRepository;
import com.university.ums.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUser;
    private final DtoMapper mapper;

    public UserResponse me() {
        return mapper.toUser(currentUser.require());
    }

    public List<UserResponse> list(Role role) {
        List<User> users = role == null
                ? userRepository.findAllByOrderByFullNameAsc()
                : userRepository.findByRoleOrderByFullNameAsc(role);
        return users.stream().map(mapper::toUser).toList();
    }

    public UserResponse get(Long id) {
        return mapper.toUser(requireUser(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = request.username().trim();
        String email = normalizeEmail(request.email());
        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("Username is already in use");
        }
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Email is already in use");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setEnabled(true);
        return mapper.toUser(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User actor = currentUser.require();
        User user = requireUser(id);
        String email = normalizeEmail(request.email());
        userRepository.findByEmail(email)
                .filter(other -> !other.getId().equals(user.getId()))
                .ifPresent(other -> {
                    throw ApiException.conflict("Email is already in use");
                });

        if (user.getId().equals(actor.getId()) && request.role() != user.getRole()) {
            throw ApiException.conflict("Cannot change your own role");
        }
        if (user.getId().equals(actor.getId()) && !request.enabled()) {
            throw ApiException.conflict("Cannot disable your own account");
        }
        if (user.getRole() == Role.ADMIN && request.role() != Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw ApiException.conflict("Cannot change the role of the last admin");
        }
        if (user.getRole() == Role.ADMIN && !request.enabled() && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw ApiException.conflict("Cannot disable the last admin");
        }
        if (user.getRole() == Role.TEACHER && request.role() != Role.TEACHER && courseRepository.existsByTeacherId(user.getId())) {
            throw ApiException.conflict("Reassign this teacher's courses before changing their role");
        }
        if (user.getRole() == Role.STUDENT && request.role() != Role.STUDENT && enrollmentRepository.existsByStudentId(user.getId())) {
            throw ApiException.conflict("Remove this student's enrollments before changing their role");
        }

        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return mapper.toUser(user);
    }

    @Transactional
    public void delete(Long id) {
        User actor = currentUser.require();
        User user = requireUser(id);
        if (user.getId().equals(actor.getId())) {
            throw ApiException.conflict("Cannot delete your own account");
        }
        if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw ApiException.conflict("Cannot delete the last admin");
        }
        if (courseRepository.existsByTeacherId(user.getId())) {
            throw ApiException.conflict("Reassign this teacher's courses before deleting them");
        }
        enrollmentRepository.deleteByStudentId(user.getId());
        userRepository.deleteById(user.getId());
    }

    private User requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
