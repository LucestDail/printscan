package com.baeksang.printscan.service;

import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.entity.Role;
import com.baeksang.printscan.exception.BusinessException;
import com.baeksang.printscan.exception.EntityNotFoundException;
import com.baeksang.printscan.model.request.UpdateProfileRequest;
import com.baeksang.printscan.model.request.AccountStatusRequest;
import com.baeksang.printscan.model.response.UserProfileResponse;
import com.baeksang.printscan.repository.UserRepository;
import com.baeksang.printscan.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    @Transactional
    public User createUser(String username, String password, String name, String email, String phone, String department, String position) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("이미 존재하는 사용자명입니다.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("User role not found"));

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .name(name)
                .email(email)
                .phone(phone)
                .department(department)
                .position(position)
                .roles(new HashSet<>(Arrays.asList(userRole)))
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long userId, String name, String email, String phone, String department, String position) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setDepartment(department);
        user.setPosition(position);

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }
        userRepository.deleteById(userId);
    }

    @Transactional
    public User updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        user.getRoles().clear();
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    @Transactional
    public User updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public User updateStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public void handleLoginAttempt(String username, boolean success) {
        User user = findUserByUsername(username);

        if (success) {
            userRepository.resetLoginAttempts(username, LocalDateTime.now());
        } else {
            userRepository.incrementLoginAttempts(username);
            
            if (user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
                user.setStatus("LOCKED");
                userRepository.save(user);
                throw new BusinessException("Account has been locked due to too many failed attempts");
            }
        }
    }

    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = findUserById(userId);
        user.setStatus(status);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
        return UserProfileResponse.fromUser(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail()) && 
                userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("이미 사용 중인 이메일입니다.");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        // 비밀번호 변경 처리
        if (request.getCurrentPassword() != null && request.getNewPassword() != null) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return UserProfileResponse.fromUser(userRepository.save(user));
    }

    @Transactional
    public void updateAccountStatus(String username, AccountStatusRequest request) {
        User user = findUserByUsername(username);
        
        // 상태 변경 이력을 남기는 로직을 추가할 수 있습니다.
        user.setStatus(request.getStatus());
        
        if ("DELETED".equals(request.getStatus())) {
            user.setEnabled(false);
        }
        
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String username) {
        User user = findUserByUsername(username);
        user.setStatus("DELETED");
        user.setEnabled(false);
        // 실제 삭제 대신 논리적 삭제 수행
        userRepository.save(user);
    }

    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserProfileResponse::fromUser);
    }

    public Page<UserProfileResponse> searchUsers(String keyword, Pageable pageable) {
        return userRepository.findByUsernameContainingOrNameContainingOrEmailContaining(
                keyword, keyword, keyword, pageable)
                .map(UserProfileResponse::fromUser);
    }

    @Transactional
    public void unlockAccount(String username) {
        User user = findUserByUsername(username);
        user.setStatus("ACTIVE");
        user.setLoginAttempts(0);
        userRepository.save(user);
    }
} 