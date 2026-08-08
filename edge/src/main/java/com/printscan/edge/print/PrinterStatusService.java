package com.printscan.edge.print;

import com.printscan.edge.config.PrinterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 프린터 상태 질의. network(9100) 모드에서만 양방향 소켓으로 ~HQES 응답을 읽는다.
 * USB/CUPS 는 단방향이라 미지원(supported=false). 무인 운영 시 용지없음/헤드열림 감지에 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrinterStatusService {

    private final PrinterProperties props;

    public Map<String, Object> query() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", props.getMode());
        if (!"network".equals(props.getMode())) {
            out.put("supported", false);
            out.put("note", "상태 질의는 network(9100) 모드에서만 지원됩니다. USB/CUPS는 단방향.");
            return out;
        }
        out.put("supported", true);
        out.put("host", props.getHost());
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(props.getHost(), props.getPort()), props.getTimeoutMs());
            s.setSoTimeout(2000);
            OutputStream os = s.getOutputStream();
            os.write("~HQES\r\n".getBytes(StandardCharsets.US_ASCII)); // Host Query: Error Status
            os.flush();
            InputStream is = s.getInputStream();
            byte[] buf = new byte[1024];
            int n = is.read(buf);
            String raw = n > 0 ? new String(buf, 0, n, StandardCharsets.US_ASCII) : "";
            out.put("raw", raw.trim());
            String up = raw.toUpperCase();
            out.put("paperOut", up.contains("PAPER OUT") || up.contains("MEDIA OUT"));
            out.put("headOpen", up.contains("HEAD OPEN"));
            out.put("paused", up.contains("PAUSED"));
            out.put("online", !raw.isBlank());
        } catch (Exception e) {
            out.put("online", false);
            out.put("error", e.getMessage());
            log.debug("[printer-status] 질의 실패: {}", e.getMessage());
        }
        return out;
    }
}
