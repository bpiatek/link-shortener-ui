package pl.bpiatek.linkshortenerui.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class HomeController {

    @GetMapping("/")
    String home(@CookieValue(value = "jwt", required = false) String jwt) {
        return "index";
    }

    @GetMapping("/404")
    String notFound() {
        return "404";
    }
}