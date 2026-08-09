package com.printscan.cloud.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CloudTemplateRepository extends JpaRepository<CloudTemplate, Long> {
    List<CloudTemplate> findByOrgIdOrderByNameAsc(Long orgId);
}
