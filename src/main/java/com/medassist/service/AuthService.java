package com.medassist.service;

import com.medassist.dto.LoginRequest;
import com.medassist.dto.LoginResponse;
import com.medassist.dto.UserDTO;
import com.medassist.entity.AdminUser;
import com.medassist.exception.UnauthorizedException;
import com.medassist.repository.AdminUserRepository;
import com.medassist.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;


    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getClinic().getId(),
                user.getRole().name()
        );

        UserDTO userDTO = UserDTO.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .clinicId(user.getClinic().getId().toString())
                .clinicName(user.getClinic().getName())
                .role(user.getRole())
                .build();

        logger.info("User logged in: {} ({})", user.getEmail(), user.getRole());

        return LoginResponse.builder()
                .token(token)
                .user(userDTO)
                .build();
    }
}
