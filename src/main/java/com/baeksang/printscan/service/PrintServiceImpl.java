package com.baeksang.printscan.service;

import com.baeksang.printscan.dto.PrintRequestDTO;
import com.baeksang.printscan.entity.*;
import com.baeksang.printscan.repository.PrintJobRepository;
import com.baeksang.printscan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrintServiceImpl implements PrintService {

    private final PrintJobRepository printJobRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final String UPLOAD_DIR = "uploads";

    @Override
    @Transactional
    public PrintJob createPrintJob(PrintRequestDTO request, List<MultipartFile> files) throws IOException {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        PrintJob printJob = PrintJob.builder()
                .user(user)
                .title(request.getTitle())
                .copies(request.getCopies())
                .priority(request.getPriority())
                .doubleSided(request.getDoubleSided())
                .color(request.getColor())
                .stapled(request.getStapled())
                .notes(request.getNotes())
                .status(PrintJobStatus.PENDING)
                .build();

        // 파일 저장 및 PrintJobFile 엔티티 생성
        if (files != null && !files.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            for (MultipartFile file : files) {
                String storedFilename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(storedFilename);
                Files.copy(file.getInputStream(), filePath);

                PrintJobFile printJobFile = PrintJobFile.builder()
                        .printJob(printJob)
                        .originalFilename(file.getOriginalFilename())
                        .storedFilename(storedFilename)
                        .fileSize(file.getSize())
                        .build();

                printJob.addFile(printJobFile);
            }
        }

        printJobRepository.save(printJob);

        // 알림 전송
        notificationService.sendNotification(
                user,
                "인쇄 작업 생성",
                String.format("인쇄 작업 '%s'가 생성되었습니다.", printJob.getTitle())
        );

        return printJob;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrintJob> getPrintJobsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return printJobRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PrintJob getPrintJob(Long id, String username) {
        PrintJob printJob = printJobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("인쇄 작업을 찾을 수 없습니다."));

        if (!printJob.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("해당 인쇄 작업에 접근할 권한이 없습니다.");
        }

        return printJob;
    }

    @Override
    @Transactional
    public void cancelPrintJob(Long id, String username) {
        PrintJob printJob = getPrintJob(id, username);
        printJob.cancel();

        // 알림 전송
        notificationService.sendNotification(
                printJob.getUser(),
                "인쇄 작업 취소",
                String.format("인쇄 작업 '%s'가 취소되었습니다.", printJob.getTitle())
        );
    }
} 