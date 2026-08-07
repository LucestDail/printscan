package com.printscan.edge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 이 디바이스가 담당하는 생산 라인 식별 — 소비(출고) 이력 귀속 및 허브 라인별 집계용. */
@Getter
@Setter
@ConfigurationProperties(prefix = "printscan.line")
public class LineProperties {
    /** 라인 이름(예: "1라인", "포장1"). */
    private String name = "라인-1";
}
