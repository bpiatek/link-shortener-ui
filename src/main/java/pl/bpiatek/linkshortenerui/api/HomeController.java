package pl.bpiatek.linkshortenerui.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import pl.bpiatek.linkshortenerui.exception.TokenChecker;

@Controller
class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final TokenRefresher tokenRefresher;
    private final TokenChecker tokenChecker;

    public HomeController(TokenRefresher tokenRefresher, TokenChecker tokenChecker) {
        this.tokenRefresher = tokenRefresher;
        this.tokenChecker = tokenChecker;
    }

    @GetMapping("/")
    public String home(Model model,
                       @CookieValue(value = "jwt", required = false) String jwt) {

        if (jwt == null || jwt.isBlank()) {
            return "index";
        }

        if (tokenChecker.isTokenExpired(jwt)) {
            log.info("JWT is expired. Attempting to refresh...");
            try {
                var newJwt = tokenRefresher.refreshAccessToken();
                var email = tokenChecker.extractEmail(newJwt);
                if (email != null) {
                    model.addAttribute("userEmail", email);
                    log.info("Token refreshed successfully. User: {}", email);
                }
            } catch (Exception e) {
                log.warn("Failed to refresh token on index page. Treating user as logged out.", e);
                tokenRefresher.clearCookies();
                model.addAttribute("userEmail", null);
            }
        }

        return "index";
    }
}