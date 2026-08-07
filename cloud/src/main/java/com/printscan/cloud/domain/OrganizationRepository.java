package com.printscan.cloud.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByApiKey(String apiKey);
    Optional<Organization> findByName(String name);
}
