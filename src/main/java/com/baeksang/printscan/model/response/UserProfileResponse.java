package com.baeksang.printscan.model.response;

import com.baeksang.printscan.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String department;
    private String position;
    private String role;
    private String avatarUrl;
    private String status;
    private String lastLoginAt;

    public static UserProfileResponse fromUser(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .department(user.getDepartment())
                .position(user.getPosition())
                .role(user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.joining(", ")))
                .avatarUrl(user.getProfileImageUrl())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                .build();
    }
} 