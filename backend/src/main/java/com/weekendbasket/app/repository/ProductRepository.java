package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByAvailableTrue(Pageable pageable);
    List<Product> findByAvailableTrue();
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByCategoryIdAndAvailableTrue(Long categoryId);
    long countByAvailableTrue();

    @Query(value = "SELECT * FROM product p WHERE p.available = true " +
           "AND (:categoryId IS NULL OR p.category_id = :categoryId) " +
           "AND (:search IS NULL OR p.name ILIKE CONCAT('%', :search, '%'))",
           countQuery = "SELECT COUNT(*) FROM product p WHERE p.available = true " +
           "AND (:categoryId IS NULL OR p.category_id = :categoryId) " +
           "AND (:search IS NULL OR p.name ILIKE CONCAT('%', :search, '%'))",
           nativeQuery = true)
    Page<Product> findAvailableFiltered(
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.available = true " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:search IS NULL OR p.name ILIKE :searchPattern)")
    List<Product> findAvailableFilteredEager(
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            @Param("searchPattern") String searchPattern);

    @Query("SELECT p FROM Product p JOIN FETCH p.category ORDER BY p.category.displayOrder, p.name")
    List<Product> findAllWithCategory();
}
