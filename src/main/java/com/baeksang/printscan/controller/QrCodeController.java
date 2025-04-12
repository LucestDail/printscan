package com.baeksang.printscan.controller;

import com.baeksang.printscan.entity.QrCode;
import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.service.QrCodeService;
import com.baeksang.printscan.service.UserService;
import com.google.zxing.WriterException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/qrcodes")
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeService qrCodeService;
    private final UserService userService;

    @PostMapping("/{type}")
    public ResponseEntity<QrCode> generateQrCode(
            @PathVariable String type,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        QrCode qrCode = qrCodeService.generateQrCode(user, type.toUpperCase());
        return ResponseEntity.ok(qrCode);
    }

    @GetMapping("/{code}/image")
    public ResponseEntity<byte[]> getQrCodeImage(@PathVariable String code) {
        try {
            byte[] qrCodeImage = qrCodeService.generateQrCodeImage(code);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return new ResponseEntity<>(qrCodeImage, headers, HttpStatus.OK);
        } catch (WriterException | IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{code}/validate")
    public ResponseEntity<QrCode> validateQrCode(@PathVariable String code) {
        QrCode qrCode = qrCodeService.validateAndUseQrCode(code);
        return ResponseEntity.ok(qrCode);
    }

    @GetMapping
    public ResponseEntity<Page<QrCode>> getUserQrCodes(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(qrCodeService.getUserQrCodes(user, pageable));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<Page<QrCode>> getUserQrCodesByType(
            @PathVariable String type,
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(qrCodeService.getUserQrCodesByType(user, type.toUpperCase(), pageable));
    }
} 