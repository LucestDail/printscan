package com.baeksang.printscan.service.print;

import com.baeksang.printscan.config.PrinterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * USB Zebra 장치 노드(/dev/usb/lp0 등)에 raw ZPL 직접 write.
 * .25 서비스 유저가 해당 장치 쓰기 권한(lp 그룹/udev) 필요.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RawDevicePrintTransport implements PrintTransport {

    private final PrinterProperties props;

    @Override
    public String mode() {
        return "rawdev";
    }

    @Override
    public void send(String zpl) throws Exception {
        Path dev = Path.of(props.getDevice());
        if (!Files.exists(dev)) {
            throw new IllegalStateException("장치 노드가 없습니다: " + props.getDevice()
                    + " (ls -l /dev/usb/lp* 로 확인, 권한은 lp 그룹).");
        }
        try (OutputStream os = Files.newOutputStream(dev, StandardOpenOption.WRITE)) {
            os.write(zpl.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        log.info("[print] rawdev 전송 완료: {} ({} bytes)", props.getDevice(), zpl.length());
    }
}
