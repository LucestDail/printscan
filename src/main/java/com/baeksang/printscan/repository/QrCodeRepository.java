package com.baeksang.printscan.repository;

import com.baeksang.printscan.entity.QrCode;
import com.baeksang.printscan.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, Long> {
    Optional<QrCode> findByCode(String code);
    Page<QrCode> findByUser(User user, Pageable pageable);
    Page<QrCode> findByUserAndType(User user, String type, Pageable pageable);
    
    @Query("SELECT q FROM QrCode q WHERE q.code = :code AND q.used = false AND q.expiresAt > :now")
    Optional<QrCode> findValidQrCode(@Param("code") String code, @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(q) FROM QrCode q WHERE q.user = :user AND q.used = false AND q.expiresAt > :now")
    long countActiveQrCodes(@Param("user") User user, @Param("now") LocalDateTime now);
} 