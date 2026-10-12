package com.groceryshop.repository;

import com.groceryshop.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByIsActiveTrue();
    Optional<Brand> findByName(String name);
}
