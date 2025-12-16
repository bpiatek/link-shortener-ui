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

    /**
     * Executes an API call with automatic Token Refresh logic.
     *
     * @param apiCall A function that takes the JWT (String) and returns the result (T).
     * @return The result of the API call.
     */
    public <T> T execute(Function<String, T> apiCall) {
        String accessToken = getCookieValue("jwt");

        if (accessToken == null || accessToken.isBlank()) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }

        try {
            return apiCall.apply(accessToken);
        } catch (HttpClientErrorException.Unauthorized e) {
            String newAccessToken = performRefresh();
            return apiCall.apply(newAccessToken);
        }
    }

    private String performRefresh() {
        String refreshToken = getCookieValue("refresh_jwt");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new HttpClientErrorException(org.springframework.http.HttpStatus.UNAUTHORIZED, "No refresh token available");
        }

        try {
            // Call Gateway: POST /users/auth/refresh
            var newTokens = restClient.post()
                    .uri("/users/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RefreshRequest(refreshToken))
                    .retrieve()
                    .body(TokenResponse.class);

            // Update Cookies in the Browser immediately
            updateCookie("jwt", newTokens.accessToken(), 900); // 15 min
            updateCookie("refresh_jwt", newTokens.refreshToken(), 604800); // 7 days

            return newTokens.accessToken();

        } catch (Exception e) {
            // If refresh fails (token expired/revoked), clear cookies and re-throw
            clearCookies();
            throw new HttpClientErrorException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Refresh failed");
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
        cookie.setSecure(true); // Important for HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(age);
        httpResponse.addCookie(cookie);
    }

    private void clearCookies() {
        updateCookie("jwt", "", 0);
        updateCookie("refresh_jwt", "", 0);
    }
}