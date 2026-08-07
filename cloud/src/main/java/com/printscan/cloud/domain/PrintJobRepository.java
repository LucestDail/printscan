package com.printscan.cloud.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PrintJobRepository extends JpaRepository<PrintJobCloud, Long> {
    Optional<PrintJobCloud> findFirstByDeviceIdAndStatusOrderByIdAsc(Long deviceId, PrintJobCloud.Status status);
    List<PrintJobCloud> findTop20ByOrderByIdDesc();
    long countByStatus(PrintJobCloud.Status status);
}
