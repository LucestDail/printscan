package com.printscan.edge.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;

/** 로컬 H2 DB 정기 백업(BACKUP TO). Pi SD카드/전원손상 대비. Postgres 등은 pg_dump 로 별도(스킵). */
@Slf4j
@Component
public class BackupService {

    private final JdbcTemplate jdbc;
    private final String dbUrl;
    private final String dir;
    private final AlertService alerts;

    public BackupService(JdbcTemplate jdbc,
                         @Value("${spring.datasource.url:}") String dbUrl,
                         @Value("${printscan.backup.dir:./data/backup}") String dir,
                         AlertService alerts) {
        this.jdbc = jdbc;
        this.dbUrl = dbUrl;
        this.dir = dir;
        this.alerts = alerts;
    }

    @Scheduled(cron = "${printscan.backup.cron:0 0 3 * * *}") // 매일 03:00
    public void backup() {
        if (dbUrl == null || !dbUrl.startsWith("jdbc:h2")) {
            log.debug("[backup] H2 아님 → 스킵(외부 DB는 pg_dump 등 별도)");
            return;
        }
        try {
            new File(dir).mkdirs();
            String path = new File(dir, "printscan-" + LocalDate.now() + ".zip").getAbsolutePath();
            jdbc.execute("BACKUP TO '" + path + "'");
            log.info("[backup] 완료: {}", path);
        } catch (Exception e) {
            alerts.alert("ERROR", "backup", "DB 백업 실패: " + e.getMessage());
        }
    }
}
