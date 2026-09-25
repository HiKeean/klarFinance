package com.api.klarfinance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@Data
@ConfigurationProperties(prefix = "app")
public class AppConfigProperties {
    private Security security;
    private Redis redis;
    private Kirimi kirimi;
    private Deskcall deskcall;

    @Data
    public static class Security{
        private String password;
        private String jwtSecret;
        private Long jwtTtlMinutes;
        private Integer cryptoKey;
        private List<String> corsAllowedOrigins;
    }

    @Data
    public static class Redis{
        private String keyPrefix;
        private String host;
        private Integer port;
        private Duration timeout;
        private Boolean sslEnabled;
        private String username;
        private String password;
        private Integer lettucePoolMaxActive;
        private Duration lettucePoolMaxWait;
        private Integer lettucePoolMaxIdle;
        private Integer lettucePoolMinIdle;
    }

    @Data
    public static class Kirimi {
        private String url = "https://api.kirimi.id/v1/send-message";
        private String userCode;
        private String secret;
        private String deviceId;
        /** false = jangan kirim WhatsApp beneran, cuma log isinya (buat dev/testing). Default true. */
        private Boolean enabled = true;
    }

    /** deskcall (service voice agent penagihan terpisah) - dipanggil server-to-server dari tombol
     * Call di NPL Report webadmin (demo). Lihat deskcall/DeskcallClient. */
    @Data
    public static class Deskcall {
        private String baseUrl = "http://localhost:8990";
        private String apiKey;
        private String aiName = "Babi";
        private String companyName = "KlarFinance";
        /** false = pembuka demo tanpa "asisten digital"; deskcall menolaknya kecuali server deskcall
         * diset DESKCALL_ALLOW_DEMO_OPENING=true. Default true (AI mengaku asisten digital). */
        private Boolean discloseAi = true;
    }
}
