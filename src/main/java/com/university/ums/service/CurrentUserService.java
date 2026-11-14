package com.university.ums.service;

import com.university.ums.domain.User;
import com.university.ums.exception.ApiException;
import com.university.ums.repo.UserRepository;
import com.university.ums.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw ApiException.unauthorized("Authentication required");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> ApiException.unauthorized("Authentication required"));
    }
}
