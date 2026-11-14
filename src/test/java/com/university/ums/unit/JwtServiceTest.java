package com.university.ums.unit;

import com.university.ums.domain.Role;
import com.university.ums.domain.User;
import com.university.ums.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("unit-test-jwt-secret-key-32bytes!!", 3_600_000L);
    }

    @Test
    void generateTokenRoundTripsUsername() {
        User user = new User();
        user.setUsername("teacher");
        user.setRole(Role.TEACHER);

        String token = jwtService.generateToken(user);

        assertEquals("teacher", jwtService.extractUsername(token));
        assertNotEquals("teacher", token);
    }
}
