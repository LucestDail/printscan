package com.printscan.edge.cloud;

import com.printscan.edge.config.LineProperties;
import com.printscan.edge.config.PrinterProperties;
import com.printscan.edge.inventory.InventoryService;
import com.printscan.edge.label.LabelService;
import com.printscan.edge.label.LabelTemplateRepository;
import com.printscan.edge.label.RenderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * 클라우드 동기화 HTTP 계약 검증(MockRestServiceServer). 등록 파싱·잡 멱등(재인쇄 방지)·신규잡 인쇄 왕복.
 * 감사 지적 "sync 0% 커버리지" 해소.
 */
class CloudSyncClientContractTest {

    private CloudSyncProperties props;
    private DeviceIdentityRepository identityRepo;
    private LabelService labelService;
    private InventoryService inventory;
    private PrintedJobRepository printedJobs;
    private LabelTemplateRepository templates;
    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        props = new CloudSyncProperties();
        props.setEnabled(true);
        props.setBaseUrl("http://hub.local");
        identityRepo = mock(DeviceIdentityRepository.class);
        labelService = mock(LabelService.class);
        inventory = mock(InventoryService.class);
        printedJobs = mock(PrintedJobRepository.class);
        templates = mock(LabelTemplateRepository.class);
        PrinterProperties printer = new PrinterProperties();
        LineProperties line = new LineProperties();
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        client = new CloudSyncClient(props, identityRepo, labelService, inventory,
                printer, line, printedJobs, templates, builder);
    }

    private CloudSyncClient client;

    @Test
    void 등록_응답_파싱_및_영속() {
        when(identityRepo.findById(1L)).thenReturn(Optional.empty());
        server.expect(requestTo("http://hub.local/api/device/register"))
              .andExpect(method(org.springframework.http.HttpMethod.POST))
              .andRespond(withSuccess("{\"deviceToken\":\"TOK-1\",\"deviceId\":5}", MediaType.APPLICATION_JSON));

        client.init();  // token 없음 → register 수행

        ArgumentCaptor<DeviceIdentity> cap = ArgumentCaptor.forClass(DeviceIdentity.class);
        verify(identityRepo).save(cap.capture());
        org.junit.jupiter.api.Assertions.assertEquals("TOK-1", cap.getValue().getDeviceToken());
        org.junit.jupiter.api.Assertions.assertEquals(5L, cap.getValue().getCloudDeviceId());
        server.verify();
    }

    @Test
    void 이미인쇄한_잡_재인쇄금지_ack만() throws Exception {
        seedToken();
        client.init();  // 토큰 로드(register HTTP 없음)

        server.expect(requestTo("http://hub.local/api/device/jobs/next"))
              .andRespond(withSuccess("{\"id\":10,\"copies\":1,\"widthMm\":40,\"heightMm\":25,\"dpi\":203,\"elementsJson\":\"[]\",\"variablesJson\":\"{}\"}", MediaType.APPLICATION_JSON));
        when(printedJobs.existsById(10L)).thenReturn(true);            // 이미 인쇄됨
        server.expect(requestTo("http://hub.local/api/device/jobs/10/ack"))
              .andExpect(method(org.springframework.http.HttpMethod.POST))
              .andRespond(withSuccess());

        client.pollJobs();

        verify(labelService, never()).print(any(RenderRequest.class));  // 재인쇄 금지(멱등)
        verify(printedJobs, never()).save(any());
        server.verify();                                                // ack 는 전송됨
    }

    @Test
    void 신규잡_인쇄_기록_ack() throws Exception {
        seedToken();
        client.init();

        server.expect(requestTo("http://hub.local/api/device/jobs/next"))
              .andRespond(withSuccess("{\"id\":11,\"copies\":1,\"widthMm\":40,\"heightMm\":25,\"dpi\":203,\"elementsJson\":\"[]\",\"variablesJson\":\"{}\"}", MediaType.APPLICATION_JSON));
        when(printedJobs.existsById(11L)).thenReturn(false);
        server.expect(requestTo("http://hub.local/api/device/jobs/11/ack"))
              .andExpect(method(org.springframework.http.HttpMethod.POST))
              .andRespond(withSuccess());

        client.pollJobs();

        verify(labelService, times(1)).print(any(RenderRequest.class)); // 인쇄 수행
        ArgumentCaptor<PrintedJob> cap = ArgumentCaptor.forClass(PrintedJob.class);
        verify(printedJobs).save(cap.capture());                        // 인쇄 확정 기록(멱등키)
        org.junit.jupiter.api.Assertions.assertEquals(11L, cap.getValue().getJobId());
        server.verify();
    }

    @Test
    void 토큰거부_401시_신원폐기하고_다음주기_재등록() {
        seedToken();
        client.init();  // 토큰 TOK-1 로드

        // 모든 기대를 선언순서로 미리 등록(요청 실행 후 추가 불가). 동일 URI 는 선언순서로 매칭.
        server.expect(requestTo("http://hub.local/api/device/jobs/next"))
              .andRespond(withStatus(HttpStatus.UNAUTHORIZED));                      // 1차 폴링 → 401
        server.expect(requestTo("http://hub.local/api/device/register"))
              .andExpect(method(org.springframework.http.HttpMethod.POST))
              .andRespond(withSuccess("{\"deviceToken\":\"TOK-2\",\"deviceId\":9}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://hub.local/api/device/jobs/next"))
              .andRespond(withStatus(HttpStatus.NO_CONTENT));                        // 2차 폴링(재등록 후) → 잡 없음

        client.pollJobs();   // 401 → 신원 폐기(token=null)
        client.pollJobs();   // token null → 재등록 후 정상 폴링

        verify(identityRepo).deleteById(1L);   // 로컬 신원 폐기
        ArgumentCaptor<DeviceIdentity> cap = ArgumentCaptor.forClass(DeviceIdentity.class);
        verify(identityRepo).save(cap.capture());
        org.junit.jupiter.api.Assertions.assertEquals("TOK-2", cap.getValue().getDeviceToken(), "새 토큰으로 재등록");
        server.verify();
    }

    private void seedToken() {
        DeviceIdentity d = new DeviceIdentity();
        d.setId(1L); d.setCloudDeviceId(5L); d.setDeviceToken("TOK-1");
        when(identityRepo.findById(1L)).thenReturn(Optional.of(d));
    }
}
