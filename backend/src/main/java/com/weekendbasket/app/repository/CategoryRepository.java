package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
    List<Category> findByActiveTrueOrderByDisplayOrderAsc();
}
