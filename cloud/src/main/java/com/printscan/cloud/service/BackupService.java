package com.printscan.cloud.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;

/** 허브 H2 정기 백업(BACKUP TO). Postgres(prod)는 pg_dump 별도 → 스킵. */
@Slf4j
@Component
public class BackupService {

    private final JdbcTemplate jdbc;
    private final String dbUrl;
    private final String dir;

    public BackupService(JdbcTemplate jdbc,
                        @Value("${spring.datasource.url:}") String dbUrl,
                        @Value("${printscan.backup.dir:./data/backup}") String dir) {
        this.jdbc = jdbc; this.dbUrl = dbUrl; this.dir = dir;
    }

    @Scheduled(cron = "${printscan.backup.cron:0 30 3 * * *}")
    public void backup() {
        if (dbUrl == null || !dbUrl.startsWith("jdbc:h2")) {
            log.debug("[backup] H2 아님(Postgres 등) → 스킵. pg_dump 로 별도 백업.");
            return;
        }
        try {
            new File(dir).mkdirs();
            String path = new File(dir, "cloud-" + LocalDate.now() + ".zip").getAbsolutePath();
            jdbc.execute("BACKUP TO '" + path + "'");
            log.info("[backup] 완료: {}", path);
        } catch (Exception e) {
            log.warn("[backup] 실패: {}", e.getMessage());
        }
    }
}
