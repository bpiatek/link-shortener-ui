package pl.bpiatek.linkshortenerui.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.annotation.RequestScope;
import pl.bpiatek.linkshortenerui.dto.RefreshRequest;
import pl.bpiatek.linkshortenerui.dto.TokenResponse;

import java.util.Arrays;

@Service
@RequestScope
class TokenRefresher {

    private static final Logger log = LoggerFactory.getLogger(TokenRefresher.class);

    private final RestClient restClient;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    TokenRefresher(RestClient restClient,
                   HttpServletRequest request,
                   HttpServletResponse response) {
        this.restClient = restClient;
        this.request = request;
        this.response = response;
    }

    String refreshAccessToken() {
        var refreshToken = getCookie("refresh_jwt");

        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                log.info("no refresh token");
                throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
            }

            var tokens = restClient.post()
                    .uri("/users/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RefreshRequest(refreshToken))
                    .retrieve()
                    .body(TokenResponse.class);

            log.info("refresh successful");

            setCookie("jwt", tokens.accessToken(), 900);
            setCookie("refresh_jwt", tokens.refreshToken(), 604800);

            return tokens.accessToken();
        } catch (Exception e) {
            log.error("refresh failed", e);
            clearCookies();
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Refresh failed");
        }
    }

    public void clearCookies() {
        setCookie("jwt", "", 0);
        setCookie("refresh_jwt", "", 0);
    }

    private String getCookie(String name) {
        if (request.getCookies() == null) {
            log.info("Cookies are not present");
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(c -> {
                    log.info("Cookie found: {}", name);
                    return  c.getValue();
                })
                .findAny().orElseGet(() -> {
                    log.info("Cookie not found: {}", name);
                    return null;
                });
    }

    private void setCookie(String name, String value, int maxAge) {
        var cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}