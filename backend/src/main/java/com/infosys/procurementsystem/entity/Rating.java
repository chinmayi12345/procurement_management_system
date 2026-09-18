package com.infosys.procurementsystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A rating submitted by a user for a completed purchase request / supplier.
 * Admins can view all ratings together with the id of the user who gave them.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ratings")
public class Rating extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({ "department", "hibernateLazyInitializer", "handler" })
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_request_id")
    @JsonIgnoreProperties({ "items", "hibernateLazyInitializer", "handler" })
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({ "supplier", "category", "hibernateLazyInitializer", "handler" })
    private Product product;

    @Column(name = "rating_value", nullable = false)
    private Integer ratingValue;

    @Column(length = 1000)
    private String comment;
}
