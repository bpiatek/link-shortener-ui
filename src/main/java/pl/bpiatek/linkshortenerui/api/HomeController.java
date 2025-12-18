package pl.bpiatek.linkshortenerui.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final TokenRefresher tokenRefresher;
    private final TokenExtractor tokenExtractor;

    public HomeController(TokenRefresher tokenRefresher, TokenExtractor tokenExtractor) {
        this.tokenRefresher = tokenRefresher;
        this.tokenExtractor = tokenExtractor;
    }

    @GetMapping("/")
    public String home(Model model, @CookieValue(value = "jwt", required = false) String jwt) {
        if (jwt == null || jwt.isBlank()) {
            try {
                var newJwt = tokenRefresher.refreshAccessToken();
                var email = tokenExtractor.extractEmail(newJwt);

                if (email != null) {
                    model.addAttribute("userEmail", email);
                    log.info("Token refreshed successfully. User: {}", email);
                }
            } catch (Exception e) {
                log.warn("Failed to refresh token on index page. Treating user as logged out.", e);
                model.addAttribute("userEmail", null);
            }
        }

        return "index";
    }
}