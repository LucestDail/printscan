package com.baeksang.printscan.service;

import com.baeksang.printscan.entity.QrCode;
import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.repository.QrCodeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final NotificationService notificationService;

    @Value("${qrcode.expiration-minutes:30}")
    private int expirationMinutes;

    @Value("${qrcode.size:250}")
    private int qrCodeSize;

    @Transactional
    public QrCode generateQrCode(User user, String type) {
        String uniqueCode = generateUniqueCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        QrCode qrCode = QrCode.builder()
                .user(user)
                .code(uniqueCode)
                .type(type)
                .expiresAt(expiresAt)
                .build();

        qrCode = qrCodeRepository.save(qrCode);

        notificationService.createNotification(
            user,
            "QR 코드가 생성되었습니다",
            String.format("%s용 QR 코드가 생성되었습니다. %d분 동안 유효합니다.", type, expirationMinutes),
            "INFO"
        );

        return qrCode;
    }

    public byte[] generateQrCodeImage(String code) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(code, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    @Transactional
    public QrCode validateAndUseQrCode(String code) {
        QrCode qrCode = qrCodeRepository.findValidQrCode(code, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("유효하지 않은 QR 코드입니다."));

        qrCode.markAsUsed();
        qrCodeRepository.save(qrCode);

        notificationService.createNotification(
            qrCode.getUser(),
            "QR 코드가 사용되었습니다",
            String.format("%s용 QR 코드가 사용되었습니다.", qrCode.getType()),
            "SUCCESS"
        );

        return qrCode;
    }

    public Page<QrCode> getUserQrCodes(User user, Pageable pageable) {
        return qrCodeRepository.findByUser(user, pageable);
    }

    public Page<QrCode> getUserQrCodesByType(User user, String type, Pageable pageable) {
        return qrCodeRepository.findByUserAndType(user, type, pageable);
    }

    private String generateUniqueCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
} 