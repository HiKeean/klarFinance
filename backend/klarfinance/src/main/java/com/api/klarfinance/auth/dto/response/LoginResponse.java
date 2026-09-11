package com.api.klarfinance.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserProfile userProfile;
    private List<MenuResponse> menu;

    /** Nasabah-only: "PENDING_APPLICATION" (no active limit yet) or "ACTIVE" (approved limit).
     * Null for internal/staff roles, which don't have a LimitApplication/ActiveLimit at all. */
    private String accountStatus;

    @Data @Builder
    public static class UserProfile {
        private String name;
        private String identity;
        private String role;
    }

    @Data @Builder
    public static class MenuResponse {
        private String url;
        private String name;
        private String logo;
    }
}
