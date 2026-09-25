package com.api.klarfinance.deskcall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.api.klarfinance.config.AppConfigProperties;
import com.api.klarfinance.deskcall.dto.DeskcallCreateCallResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/** Klien server-to-server ke deskcall API (kontrak: deskcall/docs/API.md). API key hanya ada di
 * backend - webadmin tidak pernah memanggil deskcall langsung. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeskcallClient {
    private final AppConfigProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder().build();

    public DeskcallCreateCallResponse createCall(Map<String, Object> callContext) {
        AppConfigProperties.Deskcall deskcall = properties.getDeskcall();
        if (deskcall == null || !StringUtils.hasText(deskcall.getApiKey())) {
            throw new IllegalStateException("deskcall belum dikonfigurasi (DESKCALL_API_KEY kosong)");
        }
        try {
            DeskcallCreateCallResponse response = restClient.post()
                    .uri(deskcall.getBaseUrl().replaceAll("/+$", "") + "/api/v1/calls")
                    .header("X-API-Key", deskcall.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(callContext)
                    .retrieve()
                    .body(DeskcallCreateCallResponse.class);
            if (response == null || !StringUtils.hasText(response.customerToken())) {
                throw new IllegalStateException("deskcall tidak mengembalikan token panggilan");
            }
            return response;
        } catch (RestClientResponseException e) {
            String detail = problemDetail(e.getResponseBodyAsString());
            log.warn("deskcall menolak create call: HTTP {} {}", e.getStatusCode().value(), detail);
            throw new IllegalStateException("deskcall menolak panggilan: " + detail);
        } catch (RestClientException e) {
            log.error("deskcall tidak bisa dihubungi di {}", deskcall.getBaseUrl(), e);
            throw new IllegalStateException("deskcall tidak bisa dihubungi, pastikan service deskcall jalan");
        }
    }

    /** Error deskcall berformat RFC 7807 (`detail`, opsional `code`). */
    private String problemDetail(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            String detail = node.path("detail").asText("");
            String code = node.path("code").asText("");
            if (StringUtils.hasText(code)) return code + (StringUtils.hasText(detail) ? " - " + detail : "");
            if (StringUtils.hasText(detail)) return detail;
        } catch (Exception ignored) {
            // bukan JSON - pakai body mentah
        }
        return StringUtils.hasText(body) ? body : "tanpa keterangan";
    }
}
