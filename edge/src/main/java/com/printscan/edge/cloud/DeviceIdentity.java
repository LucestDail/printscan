package com.printscan.edge.cloud;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 클라우드 등록 결과(디바이스 id/token) 로컬 영속 — 재시작 후 재등록 방지. 단일 행. */
@Entity
@Table(name = "device_identity")
@Getter
@Setter
public class DeviceIdentity {
    @Id
    private Long id = 1L;          // 단일 행 고정
    private Long cloudDeviceId;
    private String deviceToken;
}
