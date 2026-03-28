package com.meridian.portal.discovery.repository;

import com.meridian.portal.discovery.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findTop10ByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrderByPostedAtDesc(String name, String sku);
}
