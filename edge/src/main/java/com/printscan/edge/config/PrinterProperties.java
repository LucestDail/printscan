package com.printscan.edge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프린터 출력 경로 설정. USB/LAN 연결 방식을 mode 로 흡수한다.
 * network=IP:9100 raw · cups=호스트 CUPS raw 큐(javax.print) · rawdev=/dev/usb/lp0 직접 write.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "printscan.printer")
public class PrinterProperties {
    private String mode = "cups";
    private String host = "";
    private int port = 9100;
    private String name = "zebra,zd421";
    private String device = "/dev/usb/lp0";
    private int timeoutMs = 5000;
}
