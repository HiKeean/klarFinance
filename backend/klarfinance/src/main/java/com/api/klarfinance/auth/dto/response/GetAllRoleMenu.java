package com.api.klarfinance.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GetAllRoleMenu {
    private Long id;
    private Long roleId;
    private String role;
    private Long menuId;
    private String menu;
    private String url;
    private String logo;
    private String assignedBy;
    private LocalDateTime assignedAt;
}
