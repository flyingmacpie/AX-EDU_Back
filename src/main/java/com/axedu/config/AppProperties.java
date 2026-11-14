package com.axedu.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * axedu.* 설정.
 *
 * @param cors     프론트 주소 (예: http://localhost:5173)
 * @param storage  파일 저장 위치·서명 URL 설정
 */
@ConfigurationProperties(prefix = "axedu")
public record AppProperties(Cors cors, Storage storage) {

    public record Cors(List<String> allowedOrigins) {
    }

    /**
     * @param localDir      로컬 저장 폴더 (TODO: 운영은 오브젝트 스토리지)
     * @param publicBaseUrl 서명 URL 앞에 붙일 서버 주소 (예: http://localhost:8080)
     * @param urlTtlMinutes 서명 URL 유효 시간
     * @param signKey       서명 URL HMAC 키
     */
    public record Storage(String localDir, String publicBaseUrl, long urlTtlMinutes, String signKey) {
    }
}
