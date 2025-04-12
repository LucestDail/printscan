package com.baeksang.printscan.controller;

import com.baeksang.printscan.model.request.AccountStatusRequest;
import com.baeksang.printscan.model.response.UserProfileResponse;
import com.baeksang.printscan.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> searchUsers(
            @RequestParam String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(userService.searchUsers(keyword, pageable));
    }

    @PutMapping("/{username}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateAccountStatus(
            @PathVariable String username,
            @Valid @RequestBody AccountStatusRequest request) {
        userService.updateAccountStatus(username, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAccount(@PathVariable String username) {
        userService.deleteAccount(username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{username}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unlockAccount(@PathVariable String username) {
        userService.unlockAccount(username);
        return ResponseEntity.ok().build();
    }
} 