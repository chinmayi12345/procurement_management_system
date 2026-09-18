package com.infosys.procurementsystem.service.impl;

import com.infosys.procurementsystem.dto.rating.RatingRequest;
import com.infosys.procurementsystem.entity.PurchaseRequest;
import com.infosys.procurementsystem.entity.Rating;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.exception.ForbiddenException;
import com.infosys.procurementsystem.exception.ResourceNotFoundException;
import com.infosys.procurementsystem.repository.PurchaseRequestRepository;
import com.infosys.procurementsystem.repository.RatingRepository;
import com.infosys.procurementsystem.repository.UserRepository;
import com.infosys.procurementsystem.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;

    @Override
    @Transactional
    public Rating submitRating(RatingRequest dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(dto.getPurchaseRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase request not found with id: " + dto.getPurchaseRequestId()));

        if (!purchaseRequest.getRequester().getId().equals(userId)) {
            throw new ForbiddenException("Only the requester of this purchase request can rate it");
        }

        Rating rating = new Rating();
        rating.setUser(user);
        rating.setPurchaseRequest(purchaseRequest);
        rating.setRatingValue(dto.getRatingValue());
        rating.setComment(dto.getComment());

        if (!purchaseRequest.getItems().isEmpty()) {
            PurchaseRequest.PurchaseRequestItem firstItem = purchaseRequest.getItems().get(0);
            rating.setProduct(firstItem.getProduct());
            rating.setSupplier(firstItem.getProduct().getSupplier());
        }

        return ratingRepository.save(rating);
    }

    @Override
    public List<Rating> getAllRatings() {
        return ratingRepository.findAll();
    }

    @Override
    public List<Rating> getRatingsByUser(Long userId) {
        return ratingRepository.findByUserId(userId);
    }

    @Override
    public List<Rating> getRatingsBySupplier(Long supplierId) {
        return ratingRepository.findBySupplierId(supplierId);
    }

    @Override
    public List<Rating> getRatingsByProduct(Long productId) {
        return ratingRepository.findByProductId(productId);
    }
}
