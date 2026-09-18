package com.infosys.procurementsystem.repository;

import com.infosys.procurementsystem.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByUserId(Long userId);
    List<Rating> findBySupplierId(Long supplierId);
    List<Rating> findByProductId(Long productId);
}
