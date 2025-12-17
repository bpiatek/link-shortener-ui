package pl.bpiatek.linkshortenerui.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pl.bpiatek.linkshortenerui.dto.RefreshRequest;
import pl.bpiatek.linkshortenerui.dto.TokenResponse;

import java.util.Arrays;
import java.util.function.Function;

@Service
public class BackendApiService {

    private final RestClient restClient;
    private final HttpServletRequest httpRequest;
    private final HttpServletResponse httpResponse;

    public BackendApiService(RestClient restClient,
                             HttpServletRequest httpRequest,
                             HttpServletResponse httpResponse) {
        this.restClient = restClient;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
    }

    public <T> T execute(Function<String, T> apiCall) {
        String accessToken = getCookieValue("jwt");

        if (accessToken == null || accessToken.isBlank()) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }

        try {
            return apiCall.apply(accessToken);
        } catch (HttpClientErrorException.Unauthorized ex) {
            performRefresh();
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }
    }

    private void performRefresh() {
        String refreshToken = getCookieValue("refresh_jwt");

        if (refreshToken == null || refreshToken.isBlank()) {
            clearCookies();
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }

        try {
            TokenResponse tokens = restClient.post()
                    .uri("/users/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RefreshRequest(refreshToken))
                    .retrieve()
                    .body(TokenResponse.class);

            updateCookie("jwt", tokens.accessToken(), 900);
            updateCookie("refresh_jwt", tokens.refreshToken(), 604800);

        } catch (Exception e) {
            clearCookies();
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }
    }

    private String getCookieValue(String name) {
        if (httpRequest.getCookies() == null) return null;
        return Arrays.stream(httpRequest.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void updateCookie(String name, String value, int age) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(age);
        httpResponse.addCookie(cookie);
    }

    private void clearCookies() {
        updateCookie("jwt", "", 0);
        updateCookie("refresh_jwt", "", 0);
    }
}