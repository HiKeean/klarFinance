package com.api.klarfinance.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GetAllMenu {
    private Long id;
    private String url;
    private String name;
    private String logo;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
