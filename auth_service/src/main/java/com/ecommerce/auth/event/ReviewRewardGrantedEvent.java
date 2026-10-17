package com.ecommerce.auth.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRewardGrantedEvent implements Serializable {
    private String reviewId;
    private String userId;
    private Integer coins;
    private LocalDateTime createdAt;
}
