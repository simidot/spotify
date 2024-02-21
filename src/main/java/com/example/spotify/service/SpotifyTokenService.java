package com.example.spotify.service;

import com.example.spotify.dto.AccessTokenDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class SpotifyTokenService {
    // RequestBody는 변하지 않으므로 필드로 올린다...
    private final MultiValueMap<String, Object> parts;
    private final RestClient authRestClient;
    private LocalDateTime lastIssuedAt; //마지막으로 Token을 발급한 시점
    private String token; //토큰


    public SpotifyTokenService(
            @Value("${spotify.client-id}") String clientId,
            @Value("${spotify.client-secret}") String clientSecret
    ) {
        this.authRestClient = RestClient.builder()
                .baseUrl("https://accounts.spotify.com/api/token")
                .build();

        // RequestBody는 항상 같으므로 저장해두자.
        this.parts = new LinkedMultiValueMap<>();
        this.parts.add("grant_type", "client_credentials");
        this.parts.add("client_id", clientId);
        this.parts.add("client_secret", clientSecret);
        //일단 처음 생성될때는 토큰을 발급받아야 한다.
    }

    // Token 재발행 메서드
    public void reIssue() {
        log.info("issuing access token");
        AccessTokenDto response = authRestClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(parts)
                .retrieve()
                .body(AccessTokenDto.class);
        lastIssuedAt = LocalDateTime.now();
        token = response.getAccessToken();
        log.info("new access token issued: {}", token);
    }

    // 현재 사용중인 Token 반환
    public String getToken() {
        log.info("last issued: {}", lastIssuedAt);
        log.info("time passed: {} mins ", ChronoUnit.MINUTES.between(lastIssuedAt, LocalDateTime.now()));
        //만약 발급받은지 50분이 지났다면 재발행
        if (lastIssuedAt.isBefore(LocalDateTime.now().minusMinutes(50))) {
            reIssue();
        }
        //재발행을 했든 안했든 token 반환
        return token;
    }
}
