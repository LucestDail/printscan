package com.printscan.edge.print;

import com.printscan.edge.config.PrinterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 활성 mode 의 transport 로 ZPL 을 전송. 물리 연결(USB/LAN)은 mode 로 흡수. */
@Slf4j
@Service
public class PrintService {

    private final PrinterProperties props;
    private final Map<String, PrintTransport> transports;

    public PrintService(PrinterProperties props, List<PrintTransport> transportList) {
        this.props = props;
        this.transports = transportList.stream()
                .collect(Collectors.toMap(PrintTransport::mode, Function.identity()));
        log.info("[printer] transport={}, 활성 mode={}", transports.keySet(), props.getMode());
    }

    public void print(String zpl) throws Exception {
        PrintTransport transport = transports.get(props.getMode());
        if (transport == null) {
            throw new IllegalStateException("알 수 없는 프린터 mode: " + props.getMode()
                    + " (지원: " + transports.keySet() + ")");
        }
        transport.send(zpl);
    }
}
