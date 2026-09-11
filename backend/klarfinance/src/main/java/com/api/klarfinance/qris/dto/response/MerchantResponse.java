package com.api.klarfinance.qris.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchantResponse {
    private String merchantCode;
    private String name;
}
