package com.baeksang.printscan.repository;

import com.baeksang.printscan.entity.JobQueue;
import com.baeksang.printscan.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobQueueRepository extends JpaRepository<JobQueue, Long> {
    Page<JobQueue> findByUser(User user, Pageable pageable);
    Page<JobQueue> findByUserAndJobType(User user, String jobType, Pageable pageable);
    List<JobQueue> findByJobStatus(String status);
    
    @Query("SELECT j FROM JobQueue j WHERE j.jobStatus = 'PENDING' ORDER BY j.priority DESC, j.createdAt ASC")
    List<JobQueue> findPendingJobs();
    
    @Query("SELECT COUNT(j) FROM JobQueue j WHERE j.jobStatus = 'PROCESSING'")
    long countProcessingJobs();
} 