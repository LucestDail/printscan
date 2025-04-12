package com.baeksang.printscan.repository;

import com.baeksang.printscan.entity.PrintJob;
import com.baeksang.printscan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrintJobRepository extends JpaRepository<PrintJob, Long> {
    List<PrintJob> findByUserOrderByCreatedAtDesc(User user);
} 