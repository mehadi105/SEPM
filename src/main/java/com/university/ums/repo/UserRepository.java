package com.university.ums.repo;

import com.university.ums.domain.Role;
import com.university.ums.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByFullNameAsc();

    List<User> findByRoleOrderByFullNameAsc(Role role);

    long countByRole(Role role);
}
