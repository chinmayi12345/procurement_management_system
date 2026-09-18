package com.infosys.procurementsystem.controller;

import com.infosys.procurementsystem.common.ApiResponse;
import com.infosys.procurementsystem.dto.rating.RatingRequest;
import com.infosys.procurementsystem.entity.Rating;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.enums.Role;
import com.infosys.procurementsystem.exception.ForbiddenException;
import com.infosys.procurementsystem.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<ApiResponse<Rating>> submitRating(
            @Valid @RequestBody RatingRequest requestDto,
            @AuthenticationPrincipal User currentUser) {
        Rating rating = ratingService.submitRating(requestDto, currentUser.getId());
        return new ResponseEntity<>(new ApiResponse<>(true, "Rating submitted successfully", rating), HttpStatus.CREATED);
    }

    /**
     * Admin-only: all ratings, each carrying the id of the user who gave it.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Rating>>> getAllRatings(@AuthenticationPrincipal User currentUser) {
        requireAdmin(currentUser);
        List<Rating> ratings = ratingService.getAllRatings();
        return ResponseEntity.ok(new ApiResponse<>(true, "All ratings retrieved successfully", ratings));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<Rating>>> getRatingsBySupplier(
            @PathVariable Long supplierId,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPPLIER) {
            throw new ForbiddenException("Only a supplier or admin can access supplier ratings");
        }
        if (currentUser.getRole() == Role.SUPPLIER &&
                (currentUser.getSupplierId() == null || !currentUser.getSupplierId().equals(supplierId))) {
            throw new ForbiddenException("You can only access ratings for your supplier account");
        }
        List<Rating> ratings = ratingService.getRatingsBySupplier(supplierId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Supplier ratings retrieved successfully", ratings));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<Rating>>> getRatingsByProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPPLIER) {
            throw new ForbiddenException("Only a supplier or admin can access product ratings");
        }
        List<Rating> ratings = ratingService.getRatingsByProduct(productId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product ratings retrieved successfully", ratings));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Rating>>> getRatingsByUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        requireAdmin(currentUser);
        List<Rating> ratings = ratingService.getRatingsByUser(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ratings for user retrieved successfully", ratings));
    }

    private void requireAdmin(User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only an admin can access ratings data");
        }
    }
}
