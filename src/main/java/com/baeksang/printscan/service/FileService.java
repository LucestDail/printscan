package com.baeksang.printscan.service;

import com.baeksang.printscan.config.FileStorageConfig;
import com.baeksang.printscan.entity.FileInfo;
import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.repository.FileInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileInfoRepository fileInfoRepository;
    private final FileStorageConfig fileStorageConfig;

    @Transactional
    public FileInfo uploadFile(MultipartFile file, User user) throws IOException {
        // 파일 유효성 검사
        validateFile(file);

        // 파일 저장 경로 생성
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        Path targetLocation = getTargetLocation(fileName);

        // 파일 저장
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // 파일 정보 저장
        FileInfo fileInfo = FileInfo.builder()
                .user(user)
                .fileName(fileName)
                .originalFileName(file.getOriginalFilename())
                .fileType(StringUtils.getFilenameExtension(file.getOriginalFilename()))
                .fileSize(file.getSize())
                .filePath(targetLocation.toString())
                .fileStatus("UPLOADED")
                .build();

        return fileInfoRepository.save(fileInfo);
    }

    public Resource downloadFile(String fileName, User user) throws MalformedURLException {
        FileInfo fileInfo = fileInfoRepository.findByUserAndFileName(user, fileName)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        Path filePath = Paths.get(fileInfo.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists()) {
            return resource;
        } else {
            throw new RuntimeException("파일을 찾을 수 없습니다.");
        }
    }

    @Transactional
    public void deleteFile(String fileName, User user) throws IOException {
        FileInfo fileInfo = fileInfoRepository.findByUserAndFileName(user, fileName)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        Path filePath = Paths.get(fileInfo.getFilePath());
        Files.deleteIfExists(filePath);
        fileInfoRepository.delete(fileInfo);
    }

    public Page<FileInfo> getUserFiles(User user, Pageable pageable) {
        return fileInfoRepository.findByUser(user, pageable);
    }

    public Page<FileInfo> getUserFilesByType(User user, String fileType, Pageable pageable) {
        return fileInfoRepository.findByUserAndFileType(user, fileType, pageable);
    }

    private void validateFile(MultipartFile file) {
        // 파일 크기 검사
        if (file.getSize() > fileStorageConfig.getMaxFileSize()) {
            throw new RuntimeException("파일 크기가 너무 큽니다.");
        }

        // 파일 타입 검사
        String fileExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (!Arrays.asList(fileStorageConfig.getAllowedFileTypes()).contains(fileExtension)) {
            throw new RuntimeException("지원하지 않는 파일 형식입니다.");
        }
    }

    private String generateUniqueFileName(String originalFilename) {
        String fileExtension = StringUtils.getFilenameExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + fileExtension;
    }

    private Path getTargetLocation(String fileName) throws IOException {
        Path uploadPath = Paths.get(fileStorageConfig.getUploadDir());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        return uploadPath.resolve(fileName);
    }
} 