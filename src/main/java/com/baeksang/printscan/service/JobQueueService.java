package com.baeksang.printscan.service;

import com.baeksang.printscan.entity.FileInfo;
import com.baeksang.printscan.entity.JobQueue;
import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.repository.JobQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobQueueService {

    private final JobQueueRepository jobQueueRepository;
    private final NotificationService notificationService;
    private static final int MAX_PROCESSING_JOBS = 5;

    @Transactional
    public JobQueue createJob(User user, FileInfo fileInfo, String jobType, String jobOptions) {
        JobQueue job = JobQueue.builder()
                .user(user)
                .fileInfo(fileInfo)
                .jobType(jobType)
                .jobOptions(jobOptions)
                .build();

        job = jobQueueRepository.save(job);
        
        notificationService.createNotification(
            user,
            "작업이 등록되었습니다",
            String.format("%s 작업이 대기열에 추가되었습니다.", jobType),
            "INFO"
        );

        processNextJobs();
        return job;
    }

    @Transactional
    public void processNextJobs() {
        if (jobQueueRepository.countProcessingJobs() >= MAX_PROCESSING_JOBS) {
            return;
        }

        jobQueueRepository.findPendingJobs().stream()
                .limit(MAX_PROCESSING_JOBS - jobQueueRepository.countProcessingJobs())
                .forEach(this::processJob);
    }

    @Transactional
    public void processJob(JobQueue job) {
        job.updateStatus("PROCESSING");
        jobQueueRepository.save(job);
        
        notificationService.createNotification(
            job.getUser(),
            "작업이 시작되었습니다",
            String.format("%s 작업이 처리 중입니다.", job.getJobType()),
            "INFO"
        );

        // 임시로 작업 완료 처리
        completeJob(job);
    }

    @Transactional
    public void completeJob(JobQueue job) {
        job.updateStatus("COMPLETED");
        jobQueueRepository.save(job);
        
        notificationService.createNotification(
            job.getUser(),
            "작업이 완료되었습니다",
            String.format("%s 작업이 완료되었습니다.", job.getJobType()),
            "SUCCESS"
        );
    }

    @Transactional
    public void failJob(JobQueue job, String errorMessage) {
        job.updateStatus("FAILED");
        job.setErrorMessage(errorMessage);
        jobQueueRepository.save(job);
        
        notificationService.createNotification(
            job.getUser(),
            "작업이 실패했습니다",
            String.format("%s 작업이 실패했습니다: %s", job.getJobType(), errorMessage),
            "ERROR"
        );
    }

    public Page<JobQueue> getUserJobs(User user, Pageable pageable) {
        return jobQueueRepository.findByUser(user, pageable);
    }

    public Page<JobQueue> getUserJobsByType(User user, String jobType, Pageable pageable) {
        return jobQueueRepository.findByUserAndJobType(user, jobType, pageable);
    }
} 