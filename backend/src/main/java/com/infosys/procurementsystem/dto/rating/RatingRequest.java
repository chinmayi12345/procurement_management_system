package com.infosys.procurementsystem.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RatingRequest {

    @NotNull
    private Long purchaseRequestId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer ratingValue;

    private String comment;
}
