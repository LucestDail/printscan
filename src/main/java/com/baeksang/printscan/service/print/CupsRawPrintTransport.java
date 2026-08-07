package com.baeksang.printscan.service.print;

import com.baeksang.printscan.config.PrinterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 서버 호스트 OS(CUPS)에 등록된 프린터로 javax.print 전송(현행 방식).
 * USB Zebra 를 .25 CUPS 에 raw 큐로 등록하면 이 경로로 그대로 동작한다.
 * 프린터 이름은 PrinterProperties.name(콤마 구분, 부분일치)로 찾는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CupsRawPrintTransport implements PrintTransport {

    private final PrinterProperties props;

    @Override
    public String mode() {
        return "cups";
    }

    @Override
    public void send(String zpl) throws Exception {
        String[] names = props.getName().split("\\s*,\\s*");
        javax.print.PrintService printer = Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .filter(p -> Arrays.stream(names)
                        .anyMatch(n -> !n.isBlank() && p.getName().toLowerCase().contains(n.toLowerCase())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "CUPS 에서 프린터를 찾지 못했습니다(name=" + props.getName() + "). lpstat -p 로 등록 확인."));

        DocPrintJob job = printer.createPrintJob();
        Doc doc = new SimpleDoc(zpl.getBytes(StandardCharsets.UTF_8), DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        job.print(doc, null);
        log.info("[print] cups 전송 완료: printer='{}' ({} bytes)", printer.getName(), zpl.length());
    }
}
