package com.infosys.procurementsystem.service;

import com.infosys.procurementsystem.dto.rating.RatingRequest;
import com.infosys.procurementsystem.entity.Rating;

import java.util.List;

public interface RatingService {

    Rating submitRating(RatingRequest requestDto, Long userId);

    List<Rating> getAllRatings();

    List<Rating> getRatingsByUser(Long userId);

    List<Rating> getRatingsBySupplier(Long supplierId);

    List<Rating> getRatingsByProduct(Long productId);
}
