package com.university.ums.service;

import com.university.ums.domain.User;
import com.university.ums.dto.AuthResponse;
import com.university.ums.dto.LoginRequest;
import com.university.ums.exception.ApiException;
import com.university.ums.repo.UserRepository;
import com.university.ums.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (AuthenticationException ex) {
            throw ApiException.unauthorized("Invalid username or password");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole());
    }
}
