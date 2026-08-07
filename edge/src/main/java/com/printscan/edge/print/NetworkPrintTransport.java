package com.printscan.edge.print;

import com.printscan.edge.config.PrinterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** LAN Zebra: IP:9100 raw 소켓에 ZPL 바이트 전송(드라이버 불요). */
@Slf4j
@Component
@RequiredArgsConstructor
public class NetworkPrintTransport implements PrintTransport {

    private final PrinterProperties props;

    @Override
    public String mode() {
        return "network";
    }

    @Override
    public void send(String zpl) throws Exception {
        if (props.getHost() == null || props.getHost().isBlank()) {
            throw new IllegalStateException("network 모드인데 printscan.printer.host 가 비어 있습니다.");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(props.getHost(), props.getPort()), props.getTimeoutMs());
            OutputStream os = socket.getOutputStream();
            os.write(zpl.getBytes(StandardCharsets.UTF_8));
            os.flush();
            log.info("[print] network {}:{} ({} bytes)", props.getHost(), props.getPort(), zpl.length());
        }
    }
}
