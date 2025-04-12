package com.baeksang.printscan.repository;

import com.baeksang.printscan.entity.FileInfo;
import com.baeksang.printscan.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, Long> {
    Page<FileInfo> findByUser(User user, Pageable pageable);
    Page<FileInfo> findByUserAndFileType(User user, String fileType, Pageable pageable);
    Optional<FileInfo> findByUserAndFileName(User user, String fileName);
    List<FileInfo> findByFileStatus(String status);
    boolean existsByUserAndFileName(User user, String fileName);
} 