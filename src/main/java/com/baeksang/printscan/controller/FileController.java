package com.baeksang.printscan.controller;

import com.baeksang.printscan.entity.FileInfo;
import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.service.FileService;
import com.baeksang.printscan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final UserService userService;

    @PostMapping("/upload")
    public ResponseEntity<FileInfo> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        User user = userService.findUserByUsername(userDetails.getUsername());
        FileInfo fileInfo = fileService.uploadFile(file, user);
        return ResponseEntity.ok(fileInfo);
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        User user = userService.findUserByUsername(userDetails.getUsername());
        Resource resource = fileService.downloadFile(fileName, user);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @DeleteMapping("/{fileName}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileName,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        User user = userService.findUserByUsername(userDetails.getUsername());
        fileService.deleteFile(fileName, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<FileInfo>> getUserFiles(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(fileService.getUserFiles(user, pageable));
    }

    @GetMapping("/type/{fileType}")
    public ResponseEntity<Page<FileInfo>> getUserFilesByType(
            @PathVariable String fileType,
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(fileService.getUserFilesByType(user, fileType, pageable));
    }
} 