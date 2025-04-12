package com.baeksang.printscan.service;

import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.entity.Role;
import com.baeksang.printscan.model.request.SignInRequest;
import com.baeksang.printscan.model.request.SignUpRequest;
import com.baeksang.printscan.model.response.AuthResponse;
import com.baeksang.printscan.model.response.UserProfileResponse;
import com.baeksang.printscan.repository.UserRepository;
import com.baeksang.printscan.repository.RoleRepository;
import com.baeksang.printscan.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse signup(SignUpRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 존재하는 사용자 이름입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("User role not found"));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .roles(new HashSet<>(Arrays.asList(userRole)))
                .enabled(true)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRoles().stream()
                    .map(Role::getName)
                    .findFirst()
                    .orElse("ROLE_USER"))
                .build();
    }

    @Transactional
    public AuthResponse signin(SignInRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRoles().stream()
                    .map(Role::getName)
                    .findFirst()
                    .orElse("ROLE_USER"))
                .build();
    }

    public User registerUser(SignUpRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 존재하는 사용자명입니다.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("User role not found"));

        User user = User.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .department(request.getDepartment())
            .position(request.getPosition())
            .roles(new HashSet<>(Arrays.asList(userRole)))
            .enabled(true)
            .build();

        return userRepository.save(user);
    }

    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return UserProfileResponse.builder()
            .username(user.getUsername())
            .name(user.getName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .department(user.getDepartment())
            .position(user.getPosition())
            .role(user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("ROLE_USER"))
            .avatarUrl(user.getProfileImageUrl())
            .build();
    }
} 