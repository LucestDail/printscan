package com.baeksang.printscan.service;

import com.baeksang.printscan.config.PrinterProperties;
import com.baeksang.printscan.service.print.PrintTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 프린터 서비스 — ZPL 생성 + 활성 transport(mode)로 전송.
 * 물리 연결(USB/LAN)은 PrinterProperties.mode 로 흡수되어 상위는 print(zpl) 만 호출한다.
 */
@Slf4j
@Service
public class PrinterService {

    private final PrinterProperties props;
    private final Map<String, PrintTransport> transports;

    public PrinterService(PrinterProperties props, List<PrintTransport> transportList) {
        this.props = props;
        this.transports = transportList.stream()
                .collect(Collectors.toMap(PrintTransport::mode, Function.identity()));
        log.info("[printer] 등록된 transport={}, 활성 mode={}", transports.keySet(), props.getMode());
    }

    /** 활성 mode 의 transport 로 ZPL 을 전송한다. */
    public void print(String zpl) throws Exception {
        PrintTransport transport = transports.get(props.getMode());
        if (transport == null) {
            throw new IllegalStateException("알 수 없는 프린터 mode: " + props.getMode()
                    + " (지원: " + transports.keySet() + ")");
        }
        transport.send(zpl);
    }

    // ── ZPL 생성 ─────────────────────────────────────────────

    /** QR 코드 ZPL. */
    public String generateQrZpl(String data, int x, int y, int rotation, int cellSize) {
        return String.format("^XA\n^FO%d,%d^BQN,%d,%d^FDLA,%s^FS\n^XZ",
                x, y, rotation, cellSize, data);
    }

    /** 텍스트 ZPL. */
    public String generateTextZpl(String text, int x, int y, int fontSize) {
        return String.format("^XA\n^FO%d,%d^A0N,%d,%d^FD%s^FS\n^XZ",
                x, y, fontSize, fontSize, text);
    }

    /** QR + 텍스트 ZPL. */
    public String generateQrWithTextZpl(String data, String text) {
        return "^XA\n" +
               "^FO100,50^BQN,2,10^FDLA," + data + "^FS\n" +
               "^FO100,300^A0N,30,30^FD" + text + "^FS\n" +
               "^XZ";
    }
}
