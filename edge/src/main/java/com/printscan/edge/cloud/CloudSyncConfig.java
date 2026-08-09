package com.printscan.edge.cloud;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 클라우드 동기화용 RestClient.Builder(타임아웃 포함) 제공.
 * 빌더를 주입해 CloudSyncClient 가 baseUrl 만 붙여 build → 테스트에서 MockRestServiceServer 바인딩 가능.
 */
@Configuration
public class CloudSyncConfig {

    @Bean
    public RestClient.Builder cloudRestClientBuilder() {
        // 타임아웃 필수: 허브 반쯤열린 TCP 가 스케줄러 스레드를 무한 블록하지 않도록
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        rf.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        return RestClient.builder().requestFactory(rf);
    }
}
