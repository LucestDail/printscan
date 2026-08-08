package com.printscan.cloud.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConsumptionLogRepository extends JpaRepository<ConsumptionLog, Long> {

    // 집계는 SQL 로(테이블 전체 로드 방지). key, sum(qty).
    @Query("select coalesce(c.line,'(미지정)'), sum(c.qty) from ConsumptionLog c group by c.line order by sum(c.qty) desc")
    List<Object[]> sumByLine();

    @Query("select coalesce(c.operator,'(미지정)'), sum(c.qty) from ConsumptionLog c group by c.operator order by sum(c.qty) desc")
    List<Object[]> sumByOperator();

    @Query("select c.code, sum(c.qty) from ConsumptionLog c group by c.code order by sum(c.qty) desc")
    List<Object[]> sumByProduct();

    @Query("select coalesce(sum(c.qty),0) from ConsumptionLog c")
    long totalQty();
}
