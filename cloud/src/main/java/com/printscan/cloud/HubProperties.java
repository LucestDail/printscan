package com.printscan.cloud;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 허브 배포 모드 & 보안. 온프렘(LAN)은 경량(가드 off), SaaS(AWS)는 admin-token 설정으로 /api/admin 보호.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "printscan.hub")
public class HubProperties {
    /** onprem | saas (문서/표시용). */
    private String mode = "onprem";
    /** 설정 시 /api/admin/** 요청에 X-Admin-Token 헤더 요구. 빈 값이면 개방(온프렘 LAN). */
    private String adminToken = "";
}
