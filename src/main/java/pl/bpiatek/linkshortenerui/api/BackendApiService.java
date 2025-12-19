package pl.bpiatek.linkshortenerui.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.function.Function;

@Service
@RequestScope
class BackendApiService {

    private static final Logger log = LoggerFactory.getLogger(BackendApiService.class);

    private final TokenRefresher tokenRefresher;
    private final HttpServletRequest request;

    public BackendApiService(TokenRefresher tokenRefresher,
                             HttpServletRequest request) {
        this.tokenRefresher = tokenRefresher;
        this.request = request;
    }

    public <T> T execute(Function<String, T> apiCall) {
        var accessToken = getCookie("jwt");

        log.debug("execute(): jwt present={}, thread={}",
                accessToken != null,
                Thread.currentThread().getName());

        if (accessToken == null || accessToken.isBlank()) {
            log.debug("execute(): Access token cookie missing. Attempting refresh...");
            try {
                accessToken = tokenRefresher.refreshAccessToken();
            } catch (Exception e) {
                log.debug("execute(): Refresh failed. User must login.");
                throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
            }
        }

        try {
            return apiCall.apply(accessToken);
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.debug("execute(): received 401, attempting refresh");
            var refreshedToken = tokenRefresher.refreshAccessToken();
            log.debug("execute(): retrying API call with refreshed token");

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