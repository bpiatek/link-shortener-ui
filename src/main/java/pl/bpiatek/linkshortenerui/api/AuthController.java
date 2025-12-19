package pl.bpiatek.linkshortenerui.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pl.bpiatek.linkshortenerui.dto.LoginRequest;
import pl.bpiatek.linkshortenerui.dto.LoginResponse;
import pl.bpiatek.linkshortenerui.dto.LogoutRequest;
import pl.bpiatek.linkshortenerui.dto.RegisterBackendRequest;
import pl.bpiatek.linkshortenerui.dto.RegisterRequest;
import pl.bpiatek.linkshortenerui.exception.BackendErrorMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final RestClient apiGatewayClient;
    private final BackendErrorMapper errorMapper;

    AuthController(RestClient apiGatewayClient, BackendErrorMapper errorMapper) {
        this.apiGatewayClient = apiGatewayClient;
        this.errorMapper = errorMapper;
    }

    @GetMapping("/register")
    String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest("", "", ""));
        return "register";
    }

    @PostMapping("/register")
    String performRegister(
            @ModelAttribute RegisterRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (!request.password().equals(request.confirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
            return "register";
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            var backendRequest = new RegisterBackendRequest(request.email(), request.password());

            apiGatewayClient.post()
                    .uri("/users/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(backendRequest)
                    .retrieve()
                    .toBodilessEntity();

            var encodedEmail = URLEncoder.encode(request.email(), StandardCharsets.UTF_8);
            return "redirect:/registration-pending?email=" + encodedEmail;

        } catch (HttpClientErrorException e) {
            errorMapper.map(e, bindingResult, model);
            return "register";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred. Please try again.");
            return "register";
        }
    }

    @GetMapping("/registration-pending")
    String registrationPendingPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "registration-pending";
    }

    @GetMapping("/login")
    String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest("", ""));
        return "login";
    }

    @PostMapping("/login")
    String performLogin(
            @ModelAttribute LoginRequest request,
            BindingResult bindingResult,
            HttpServletResponse response,
            Model model
    ) {
        try {
            var tokenResponse = apiGatewayClient.post()
                    .uri("/users/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(LoginResponse.class);

            setJwtCookie(response, "jwt", tokenResponse.accessToken(), 900);
            setJwtCookie(response, "refresh_jwt", tokenResponse.refreshToken(), 604800);

            return "redirect:/dashboard";
        } catch (HttpClientErrorException e) {
            errorMapper.map(e, bindingResult, model);
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", "Login failed. Please try again.");
            return "login";
        }
    }

    @GetMapping("/verify")
    String verifyEmail(@RequestParam("token") String token, Model model) {
        try {
            apiGatewayClient.get()
                    .uri("/users/auth/verify?token={token}", token)
                    .retrieve()
                    .toBodilessEntity();

            return "verified";
        } catch (HttpClientErrorException e) {
            errorMapper.map(e, model);
            return "verification-error";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred during verification.");
            return "verification-error";
        }
    }

    @PostMapping("/sign-out")
    String performLogout(
            @CookieValue(value = "refresh_jwt", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null) {
            try {
                apiGatewayClient.post()
                        .uri("/users/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new LogoutRequest(refreshToken))
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception e) {
                log.error("Exception while logging out user: {}", e.getMessage());
            }
        }

        clearCookie(response, "jwt");
        clearCookie(response, "refresh_jwt");

        return "redirect:/login?logout=true";
    }

    @GetMapping("/inactive")
    String linkInactivePage() {
        return "link-inactive";
    }

    private void setJwtCookie(HttpServletResponse response, String name, String value, int maxAge) {
        var cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        var cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}