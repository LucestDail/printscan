package com.printscan.cloud.web;

import lombok.Getter;

/** 메시지 코드(messages*.properties 키)를 담는 예외 — GlobalExceptionHandler 가 요청 로케일로 번역. */
@Getter
public class ApiException extends IllegalArgumentException {
    private final String code;
    private final transient Object[] args;

    public ApiException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args;
    }
}
