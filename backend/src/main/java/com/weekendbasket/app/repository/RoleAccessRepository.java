package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.RoleAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoleAccessRepository extends JpaRepository<RoleAccess, Long> {

    @Query("SELECT ra FROM RoleAccess ra JOIN FETCH ra.role WHERE ra.user.id = :userId")
    List<RoleAccess> findByUserId(@Param("userId") Long userId);
}
