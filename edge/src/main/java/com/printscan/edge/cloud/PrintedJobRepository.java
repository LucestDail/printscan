package com.printscan.edge.cloud;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrintedJobRepository extends JpaRepository<PrintedJob, Long> {
}
