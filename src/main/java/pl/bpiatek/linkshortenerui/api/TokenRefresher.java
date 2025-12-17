package pl.bpiatek.linkshortenerui.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        String refreshToken = getCookie("refresh_jwt");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        }

        TokenResponse tokens = restClient.post()
                .uri("/users/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshRequest(refreshToken))
                .retrieve()
                .body(TokenResponse.class);

        setCookie("jwt", tokens.accessToken(), 900);
        setCookie("refresh_jwt", tokens.refreshToken(), 604800);

        return tokens.accessToken();
    }

    private String getCookie(String name) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
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