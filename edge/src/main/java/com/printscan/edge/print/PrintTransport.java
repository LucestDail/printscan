package com.printscan.edge.print;

/**
 * ZPL 전송 경로 추상화. 활성 mode 는 PrinterProperties 로 결정. 상위는 send(zpl) 만 호출.
 */
public interface PrintTransport {

    /** mode 키: network | cups | rawdev. */
    String mode();

    /** ZPL(바이트) 을 프린터로 전송. 실패 시 예외. */
    void send(String zpl) throws Exception;
}
