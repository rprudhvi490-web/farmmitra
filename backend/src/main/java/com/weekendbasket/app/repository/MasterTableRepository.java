package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.MasterTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MasterTableRepository extends JpaRepository<MasterTable, Long> {
    List<MasterTable> findByTypeOrderByLookupValueAsc(String type);
    boolean existsByTypeAndLookupCode(String type, String lookupCode);
}
