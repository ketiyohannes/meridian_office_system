package com.meridian.portal.discovery.repository;

import com.meridian.portal.discovery.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByNameAsc();

    Optional<Category> findByNameIgnoreCase(String name);

    List<Category> findTop10ByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
