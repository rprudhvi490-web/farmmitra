package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.RoleAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoleAccessRepository extends JpaRepository<RoleAccess, Long> {

    @Query("SELECT ra FROM RoleAccess ra JOIN FETCH ra.role WHERE ra.user.id = :userId")
    List<RoleAccess> findByUserId(@Param("userId") Long userId);

    @Query("SELECT ra FROM RoleAccess ra JOIN FETCH ra.user WHERE ra.role.roleId = :roleId")
    List<RoleAccess> findByRoleId(@Param("roleId") String roleId);

    @Modifying
    @Query("DELETE FROM RoleAccess ra WHERE ra.user.id = :userId AND ra.role.roleId = :roleId")
    void deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") String roleId);
}
