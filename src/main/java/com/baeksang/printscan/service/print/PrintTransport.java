package com.baeksang.printscan.service.print;

/**
 * ZPL 전송 경로 추상화. 구현체는 mode() 키로 구분되며, 활성 mode 는 PrinterProperties 로 결정된다.
 * USB/LAN 등 물리 연결 방식과 무관하게 상위 코드는 send(zpl) 만 호출한다.
 */
public interface PrintTransport {

    /** 이 전송 경로의 mode 키 (network | cups | rawdev). */
    String mode();

    /** ZPL 원문을 프린터로 전송한다. 실패 시 예외를 던진다. */
    void send(String zpl) throws Exception;
}
