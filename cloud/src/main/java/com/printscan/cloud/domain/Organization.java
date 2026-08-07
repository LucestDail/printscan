package com.printscan.cloud.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 테넌트. apiKey 로 디바이스가 자기 조직에 등록한다. */
@Entity
@Table(name = "organization")
@Getter
@Setter
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String apiKey;
}
