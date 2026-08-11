package com.printscan.cloud.web;

import lombok.Getter;

/** 디바이스 토큰 인증 실패 → 401. edge 가 이 응답으로 토큰 폐기를 감지해 재등록한다. */
@Getter
public class DeviceAuthException extends RuntimeException {
    private final String code;

    public DeviceAuthException(String code) {
        super(code);
        this.code = code;
    }
}
