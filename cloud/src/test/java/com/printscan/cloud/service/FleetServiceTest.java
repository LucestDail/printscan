package com.printscan.cloud.service;

import com.printscan.cloud.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 네트워크 출력 잡 상태머신 회귀: 원자 claim(1회)·ack SENT가드 멱등·리퍼 재큐. */
@DataJpaTest
class FleetServiceTest {

    @Autowired OrganizationRepository orgs;
    @Autowired DeviceRepository devices;
    @Autowired PrintJobRepository jobs;
    @Autowired InventorySnapshotRepository snaps;
    @Autowired ConsumptionLogRepository cons;
    @Autowired TestEntityManager em;

    @Autowired com.printscan.cloud.domain.CloudTemplateRepository tpls;
    private FleetService svc() { return new FleetService(orgs, devices, jobs, snaps, cons, tpls, new AlertService(""), new OrgKeyResolver(orgs)); }

    private Device device() { return device("K"); }

    private Device device(String apiKey) {
        Organization o = new Organization(); o.setName("o-" + apiKey); o.setApiKey(apiKey); orgs.save(o);
        return svc().register(apiKey, "dev", "cups", "line1");
    }

    private void enqueue(Device d) {
        svc().enqueuePrint(d.getOrgId(), d.getId(), 40, 25, 203, "[]", "{}", 1, null, null, null, null, null);
    }

    @Test
    void 클레임은_1회만_그다음은_없음() {
        FleetService s = svc();
        Device d = device();
        enqueue(d);
        PrintJobCloud j1 = s.pollNext(d);
        assertNotNull(j1);
        assertEquals(PrintJobCloud.Status.SENT, j1.getStatus());
        assertNull(s.pollNext(d), "QUEUED 소진 후엔 null");
    }

    @Test
    void ack는_SENT일때만_그리고_멱등() {
        FleetService s = svc();
        Device d = device();
        enqueue(d);
        PrintJobCloud j = s.pollNext(d);

        s.ack(d, j.getId(), true, "ok");
        em.flush(); em.clear();
        assertEquals(PrintJobCloud.Status.DONE, jobs.findById(j.getId()).get().getStatus());
        assertEquals(1, devices.findById(d.getId()).get().getPrintCount());

        // 재ack(이미 DONE) → 무시, printCount 불변
        s.ack(devices.findById(d.getId()).get(), j.getId(), true, "dup");
        em.flush(); em.clear();
        assertEquals(1, devices.findById(d.getId()).get().getPrintCount(), "중복 ack 로 카운트 증가 금지");
    }

    @Test
    void 교차_org_격리() {
        FleetService s = svc();
        Device a = device("KA");                       // org A + 장비
        Organization b = new Organization(); b.setName("B"); b.setApiKey("KB"); orgs.save(b);
        // B 스코프로 A 장비에 인쇄 지시 → 거부
        assertThrows(IllegalArgumentException.class,
                () -> s.enqueuePrint(b.getId(), a.getId(), 40, 25, 203, "[]", "{}", 1, null, null, null, null, null));
        // B 는 A 의 장비를 볼 수 없음
        assertTrue(s.allDevices(b.getId()).isEmpty());
        assertEquals(1, s.allDevices(a.getOrgId()).size());
    }

    @Test
    void 리퍼_SENT정체잡_재큐() {
        FleetService s = svc();
        Device d = device();
        enqueue(d);
        PrintJobCloud j = s.pollNext(d); // SENT, sentAt=now
        int n = jobs.requeueStale(LocalDateTime.now().plusMinutes(1)); // cutoff 미래 → sentAt<cutoff → 재큐
        assertEquals(1, n);
        em.flush(); em.clear();
        assertEquals(PrintJobCloud.Status.QUEUED, jobs.findById(j.getId()).get().getStatus());
    }

    // ── org-key 로테이션 ──

    private OrgKeyResolver resolver() { return new OrgKeyResolver(orgs); }

    @Test
    void 로테이션_신규키_등록_유예기간_직전키_공존() {
        FleetService s = svc();
        Organization o = new Organization(); o.setName("R"); o.setApiKey("OLD-KEY"); orgs.save(o);

        Map<String, Object> res = s.rotateOrgKey(o.getId(), 60); // 60분 유예
        String newKey = (String) res.get("apiKey");
        em.flush(); em.clear();

        assertNotEquals("OLD-KEY", newKey, "신규 키는 달라야");
        assertTrue(newKey.startsWith("ORG-"));
        // 신규 키로 등록 OK
        assertNotNull(s.register(newKey, "d-new", "cups", "L"));
        // 유예기간 내 직전 키로도 등록 OK(무중단)
        assertNotNull(s.register("OLD-KEY", "d-old", "cups", "L"));
    }

    @Test
    void 로테이션_유예0_직전키_즉시무효() {
        FleetService s = svc();
        Organization o = new Organization(); o.setName("R0"); o.setApiKey("OLD0"); orgs.save(o);

        String newKey = (String) s.rotateOrgKey(o.getId(), 0).get("apiKey"); // 유예 없음
        em.flush(); em.clear();

        assertTrue(resolver().resolve(newKey).isPresent(), "신규 키 유효");
        assertTrue(resolver().resolve("OLD0").isEmpty(), "직전 키 즉시 무효");
    }

    @Test
    void 만료된_직전키_거부() {
        FleetService s = svc();
        Organization o = new Organization(); o.setName("RE"); o.setApiKey("CUR"); orgs.save(o);
        s.rotateOrgKey(o.getId(), 60);
        // 직전 키 만료 시각을 과거로 강제
        Organization reloaded = orgs.findByApiKey(o.getApiKey()).orElseThrow();
        reloaded.setPreviousKeyExpiresAt(LocalDateTime.now().minusMinutes(1));
        orgs.save(reloaded);
        em.flush(); em.clear();

        assertTrue(resolver().resolve("CUR").isEmpty(), "만료된 직전 키는 거부");
    }

    @Test
    void 직전키_즉시폐기() {
        FleetService s = svc();
        Organization o = new Organization(); o.setName("RV"); o.setApiKey("CUR2"); orgs.save(o);
        s.rotateOrgKey(o.getId(), 60);
        assertTrue(resolver().resolve("CUR2").isPresent(), "폐기 전엔 유효");

        s.revokePreviousKey(o.getId());
        em.flush(); em.clear();
        assertTrue(resolver().resolve("CUR2").isEmpty(), "폐기 후 직전 키 무효");
    }
}
