package pl.bpiatek.linkshortenerui.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.function.Function;

@Service
@RequestScope
public class BackendApiService {

    private final TokenRefresher tokenRefresher;
    private final HttpServletRequest request;

    public BackendApiService(TokenRefresher tokenRefresher,
                             HttpServletRequest request) {
        this.tokenRefresher = tokenRefresher;
        this.request = request;
    }

    public <T> T execute(Function<String, T> apiCall) {
        String accessToken = getCookie("jwt");

        if (accessToken == null || accessToken.isBlank()) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }

        try {
            return apiCall.apply(accessToken);
        } catch (HttpClientErrorException.Unauthorized ex) {
            // Explicit refresh, then ONE clean retry
            String refreshedToken = tokenRefresher.refreshAccessToken();
            return apiCall.apply(refreshedToken);
        }
    }

    private String getCookie(String name) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}