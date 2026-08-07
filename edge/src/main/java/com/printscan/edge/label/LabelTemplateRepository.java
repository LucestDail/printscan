package com.printscan.edge.label;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabelTemplateRepository extends JpaRepository<LabelTemplate, Long> {
    Optional<LabelTemplate> findByName(String name);
}
