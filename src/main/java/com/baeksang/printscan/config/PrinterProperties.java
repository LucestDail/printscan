package com.baeksang.printscan.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 프린터 출력 경로 설정. .25 우분투 직결 환경에서 USB/LAN 연결 방식을 mode 로 흡수한다.
 * <ul>
 *   <li>network : 네트워크(LAN) 프린터 IP:port(9100) raw 소켓 전송</li>
 *   <li>cups    : 서버 호스트 OS(CUPS)에 등록된 프린터로 javax.print 전송 (현행, USB+CUPS raw 큐)</li>
 *   <li>rawdev  : USB 장치 노드(/dev/usb/lp0)에 raw 직접 write</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "printscan.printer")
public class PrinterProperties {
    /** network | cups | rawdev */
    private String mode = "cups";
    /** network 모드: 프린터 IP */
    private String host = "";
    /** network 모드: 포트(Zebra raw = 9100) */
    private int port = 9100;
    /** cups 모드: 프린터 이름 매칭(콤마 구분, 소문자 부분일치) */
    private String name = "zebra,zd421";
    /** rawdev 모드: USB 장치 노드 경로 */
    private String device = "/dev/usb/lp0";
    /** 연결/전송 타임아웃(ms) */
    private int timeoutMs = 5000;
}
