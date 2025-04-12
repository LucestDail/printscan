package com.baeksang.printscan.repository;

import com.baeksang.printscan.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = u.loginAttempts + 1 WHERE u.username = :username")
    void incrementLoginAttempts(@Param("username") String username);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = 0, u.lastLoginAt = :lastLoginAt WHERE u.username = :username")
    void resetLoginAttempts(@Param("username") String username, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    Page<User> findByUsernameContainingOrNameContainingOrEmailContaining(
            String username, String name, String email, Pageable pageable);
} 