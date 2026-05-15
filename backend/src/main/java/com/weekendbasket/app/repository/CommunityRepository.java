package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.Community;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    boolean existsByName(String name);
}
