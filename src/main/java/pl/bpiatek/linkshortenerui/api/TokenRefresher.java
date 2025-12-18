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
public class TokenRefresher {

    private static final Logger log = LoggerFactory.getLogger(TokenRefresher.class);

    private final RestClient restClient;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public TokenRefresher(RestClient restClient,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        this.restClient = restClient;
        this.request = request;
        this.response = response;
    }

    public String refreshAccessToken() {
        var refreshToken = getCookie("refresh_jwt");

        log.info("performRefresh(): refresh token present={}", refreshToken != null);

        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                log.info("performRefresh(): no refresh token");
                throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
            }

            var tokens = restClient.post()
                    .uri("/users/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RefreshRequest(refreshToken))
                    .retrieve()
                    .body(TokenResponse.class);

            log.info("performRefresh(): refresh successful");

            setCookie("jwt", tokens.accessToken(), 900);
            setCookie("refresh_jwt", tokens.refreshToken(), 604800);

            return tokens.accessToken();
        } catch (Exception e) {
            log.info("performRefresh(): refresh failed", e);
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
            log.info("getCookieValue({}): no cookies present", name);
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(c -> {
                    log.info("getCookieValue({}): found", name);
                    return  c.getValue();
                })
                .findAny().orElseGet(() -> {
                    log.info("getCookieValue({}): not found", name);
                    return null;
                });
    }

    private void setCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}