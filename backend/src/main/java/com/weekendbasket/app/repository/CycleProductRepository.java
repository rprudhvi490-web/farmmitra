package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.CycleProduct;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CycleProductRepository extends JpaRepository<CycleProduct, Long> {

    List<CycleProduct> findByCycleId(Long cycleId);

    Optional<CycleProduct> findByCycleIdAndProductId(Long cycleId, Long productId);

    // Used during order placement — locks the row to serialize concurrent orders
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cp FROM CycleProduct cp WHERE cp.cycle.id = :cycleId AND cp.product.id = :productId")
    Optional<CycleProduct> findByCycleIdAndProductIdForUpdate(@Param("cycleId") Long cycleId,
                                                               @Param("productId") Long productId);
}
