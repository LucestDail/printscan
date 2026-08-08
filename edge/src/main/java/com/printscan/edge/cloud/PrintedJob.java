package com.printscan.edge.cloud;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 이미 인쇄한 클라우드 잡 id 기록 — ack 실패 후 재전달 시 중복 인쇄 방지(멱등). */
@Entity
@Table(name = "printed_job")
@Getter
@Setter
public class PrintedJob {
    @Id
    private Long jobId;   // 클라우드 잡 id
}
