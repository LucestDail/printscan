package com.baeksang.printscan.repository;

import com.baeksang.printscan.entity.LabelTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabelTemplateRepository extends JpaRepository<LabelTemplate, Long> {
    Optional<LabelTemplate> findByName(String name);
}
