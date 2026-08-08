package com.printscan.edge.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.print.PrintServiceLookup;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** /actuator/health 의 'printer' 구성요소 — mode 별 프린터 도달성 실체크. */
@Component("printer")
@RequiredArgsConstructor
public class PrinterHealthIndicator implements HealthIndicator {

    private final PrinterProperties props;

    @Override
    public Health health() {
        Health.Builder b = Health.unknown().withDetail("mode", props.getMode());
        try {
            switch (props.getMode()) {
                case "cups" -> {
                    String[] names = props.getName().split("\\s*,\\s*");
                    boolean found = Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                            .anyMatch(p -> Arrays.stream(names).anyMatch(n ->
                                    !n.isBlank() && p.getName().toLowerCase().contains(n.toLowerCase())));
                    return found ? b.up().withDetail("printer", props.getName()).build()
                            : b.down().withDetail("reason", "CUPS 큐 미발견(" + props.getName() + ")").build();
                }
                case "network" -> {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress(props.getHost(), props.getPort()), props.getTimeoutMs());
                        return b.up().withDetail("host", props.getHost() + ":" + props.getPort()).build();
                    }
                }
                case "rawdev" -> {
                    return Files.exists(Path.of(props.getDevice()))
                            ? b.up().withDetail("device", props.getDevice()).build()
                            : b.down().withDetail("reason", "장치 노드 없음: " + props.getDevice()).build();
                }
                default -> { return b.down().withDetail("reason", "알 수 없는 mode").build(); }
            }
        } catch (Exception e) {
            return b.down().withDetail("error", e.getMessage()).build();
        }
    }
}
