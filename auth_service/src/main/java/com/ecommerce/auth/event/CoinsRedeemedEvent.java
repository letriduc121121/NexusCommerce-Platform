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
public class CoinsRedeemedEvent implements Serializable {
    private String orderId;
    private String userId;
    private Long coinsUsed;
    private LocalDateTime redeemedAt;
}
