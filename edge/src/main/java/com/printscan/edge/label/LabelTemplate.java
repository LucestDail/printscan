package com.printscan.edge.label;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 라벨 템플릿 — 물리 규격(mm/dpi) + 요소 배열(JSON). 요소 좌표는 mm 라 프린터 dpi 와 독립.
 * elementsJson 은 LabelElement 리스트의 직렬화. elements() 로 역직렬화해 래스터 엔진에 전달.
 */
@Entity
@Table(name = "label_template")
@Getter
@Setter
public class LabelTemplate {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    /** 클라우드 중앙 템플릿에서 동기화된 경우 원본 id(중복 방지). 로컬 전용이면 null. */
    private Long cloudId;

    /** 라벨 물리 규격(mm). 기본값은 가로형(landscape) 미디어 기준. */
    private double widthMm = 40;
    private double heightMm = 25;

    /** 프린터 해상도: 203 | 300. null 이면 앱 기본값. */
    private Integer dpi;

    /** LabelElement 리스트의 JSON 직렬화. */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String elementsJson = "[]";

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    /** elementsJson → List<LabelElement>. 파싱 실패/빈 값이면 빈 리스트. */
    @Transient
    public List<LabelElement> elements() {
        if (elementsJson == null || elementsJson.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(elementsJson, new TypeReference<List<LabelElement>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("elementsJson 파싱 실패: " + e.getMessage(), e);
        }
    }
}
