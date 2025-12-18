package pl.bpiatek.linkshortenerui.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final TokenRefresher tokenRefresher;

    public HomeController(TokenRefresher tokenRefresher) {
        this.tokenRefresher = tokenRefresher;
    }

    @GetMapping("/")
    public String home(@CookieValue(value = "jwt", required = false) String jwt) {

        if (jwt == null || jwt.isBlank()) {
            tokenRefresher.refreshAccessToken();
            return "index";
        }

        return "index";
    }
}