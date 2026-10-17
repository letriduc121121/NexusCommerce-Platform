package com.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private UUID orderItemId;
    private UUID productId;
    private UUID userId;
    private String userFullName;
    private Integer rating;
    private String comment;
    private String variantInfo;
    private Integer coinsAwarded;
    private Boolean isHidden;
    private Boolean isEdited;
    private String shopReply;
    private LocalDateTime shopRepliedAt;
    private List<String> images;
    private LocalDateTime createdAt;
}
