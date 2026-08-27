package com.skillport.server.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skillport.server.config.SkillPortProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@Service
public class WeComAuthService {
    private static final String API_BASE_URL = "https://qyapi.weixin.qq.com";
    private static final String AUTO_AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String QR_AUTHORIZE_URL = "https://open.work.weixin.qq.com/wwopen/sso/qrConnect";
    private static final long TOKEN_EXPIRY_SAFETY_SECONDS = 120;

    private final SkillPortProperties.WeCom configuration;
    private final AuthService authService;
    private final RestClient client;
    private final Object tokenLock = new Object();
    private volatile CachedAccessToken cachedAccessToken;

    public WeComAuthService(SkillPortProperties properties, AuthService authService) {
        this.configuration = properties.wecom();
        this.authService = authService;
        this.client = RestClient.builder().baseUrl(API_BASE_URL).build();
    }

    public URI authorizationUri(String mode, String state) {
        requireConfigured();
        if (state == null || state.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "授权状态不能为空");
        }
        if ("auto".equalsIgnoreCase(mode)) {
            return UriComponentsBuilder.fromUriString(AUTO_AUTHORIZE_URL)
                    .queryParam("appid", configuration.corpId())
                    .queryParam("redirect_uri", configuration.callbackUrl())
                    .queryParam("response_type", "code")
                    .queryParam("scope", "snsapi_base")
                    .queryParam("state", state)
                    .fragment("wechat_redirect")
                    .build().encode().toUri();
        }
        return UriComponentsBuilder.fromUriString(QR_AUTHORIZE_URL)
                .queryParam("appid", configuration.corpId())
                .queryParam("agentid", configuration.agentId())
                .queryParam("redirect_uri", configuration.callbackUrl())
                .queryParam("state", state)
                .build().encode().toUri();
    }

    public AuthService.SessionGrant login(String code) {
        requireConfigured();
        if (code == null || code.isBlank() || code.length() > 512) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "企业微信授权码无效");
        }
        String accessToken = accessToken();
        UserIdentityResponse identity = get("/cgi-bin/auth/getuserinfo?access_token={token}&code={code}",
                UserIdentityResponse.class, accessToken, code.trim());
        if (identity == null || identity.errCode() != 0 || identity.userId() == null || identity.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅允许当前企业成员登录");
        }
        return authService.loginWithWeCom(configuration.corpId(), identity.userId(), memberName(accessToken, identity.userId()));
    }

    public boolean configured() {
        return configuration != null && configuration.configured();
    }

    private String memberName(String accessToken, String userId) {
        MemberResponse member = get("/cgi-bin/user/get?access_token={token}&userid={userId}",
                MemberResponse.class, accessToken, userId);
        return member != null && member.errCode() == 0 && member.name() != null && !member.name().isBlank()
                ? member.name().trim()
                : "企业微信成员";
    }

    private String accessToken() {
        Instant now = Instant.now();
        CachedAccessToken current = cachedAccessToken;
        if (current != null && current.expiresAt().isAfter(now.plusSeconds(TOKEN_EXPIRY_SAFETY_SECONDS))) {
            return current.value();
        }
        synchronized (tokenLock) {
            current = cachedAccessToken;
            now = Instant.now();
            if (current != null && current.expiresAt().isAfter(now.plusSeconds(TOKEN_EXPIRY_SAFETY_SECONDS))) {
                return current.value();
            }
            AccessTokenResponse response = get("/cgi-bin/gettoken?corpid={corpId}&corpsecret={secret}",
                    AccessTokenResponse.class, configuration.corpId(), configuration.secret());
            if (response == null || response.errCode() != 0 || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "企业微信登录服务暂时不可用");
            }
            long expiresIn = Math.max(300, response.expiresIn());
            cachedAccessToken = new CachedAccessToken(response.accessToken(), now.plusSeconds(expiresIn));
            return response.accessToken();
        }
    }

    private <T> T get(String uriTemplate, Class<T> responseType, Object... uriVariables) {
        try {
            return client.get().uri(uriTemplate, uriVariables).retrieve().body(responseType);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "企业微信登录服务暂时不可用", exception);
        }
    }

    private void requireConfigured() {
        if (!configured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "企业微信登录尚未配置");
        }
    }

    private record CachedAccessToken(String value, Instant expiresAt) {
    }

    private record AccessTokenResponse(
            @JsonProperty("errcode") int errCode,
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {
    }

    private record UserIdentityResponse(
            @JsonProperty("errcode") int errCode,
            @JsonProperty("userid") String userId) {
    }

    private record MemberResponse(
            @JsonProperty("errcode") int errCode,
            @JsonProperty("name") String name) {
    }
}
