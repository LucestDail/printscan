package com.printscan.cloud.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConsumptionLogRepository extends JpaRepository<ConsumptionLog, Long> {

    // org 스코프 집계(테넌트 격리). key, sum(qty).
    @Query("select coalesce(c.line,'(미지정)'), sum(c.qty) from ConsumptionLog c where c.orgId=:orgId group by c.line order by sum(c.qty) desc")
    List<Object[]> sumByLine(@Param("orgId") Long orgId);

    @Query("select coalesce(c.operator,'(미지정)'), sum(c.qty) from ConsumptionLog c where c.orgId=:orgId group by c.operator order by sum(c.qty) desc")
    List<Object[]> sumByOperator(@Param("orgId") Long orgId);

    @Query("select c.code, sum(c.qty) from ConsumptionLog c where c.orgId=:orgId group by c.code order by sum(c.qty) desc")
    List<Object[]> sumByProduct(@Param("orgId") Long orgId);

    @Query("select coalesce(sum(c.qty),0) from ConsumptionLog c where c.orgId=:orgId")
    long totalQty(@Param("orgId") Long orgId);
}
