package pl.bpiatek.linkshortenerui.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pl.bpiatek.linkshortenerui.dto.ForgotPasswordRequest;
import pl.bpiatek.linkshortenerui.dto.LoginRequest;
import pl.bpiatek.linkshortenerui.dto.LoginResponse;
import pl.bpiatek.linkshortenerui.dto.RegisterBackendRequest;
import pl.bpiatek.linkshortenerui.dto.RegisterRequest;
import pl.bpiatek.linkshortenerui.dto.ResetPasswordBackendRequest;
import pl.bpiatek.linkshortenerui.dto.ResetPasswordForm;
import pl.bpiatek.linkshortenerui.exception.ApiError;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
class AuthController {

    private final RestClient apiGatewayClient;

    AuthController(RestClient apiGatewayClient) {
        this.apiGatewayClient = apiGatewayClient;
    }

    @GetMapping("/register")
    String registerPage(Model model) {
        // Add empty object for form binding
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
            handleBackendError(e, bindingResult, model);
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
            // 3. Handle Errors (401, 404, 400)
            handleBackendError(e, bindingResult, model);
            return "login"; // Return to login page with errors
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
            model.addAttribute("error", "The verification link is invalid or has expired.");
            return "verification-error";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred during verification.");
            return "verification-error";
        }
    }

    @GetMapping("/forgot-password")
    String forgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest(""));
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    String performForgotPassword(
            @ModelAttribute ForgotPasswordRequest request,
            Model model
    ) {
        try {

            apiGatewayClient.post()
                    .uri("/users/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            String encodedEmail = URLEncoder.encode(request.email(), StandardCharsets.UTF_8);
            return "redirect:/forgot-password-pending?email=" + encodedEmail;

        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred. Please try again later.");
            return "forgot-password";
        }
    }

    @GetMapping("/forgot-password-pending")
    String forgotPasswordPendingPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "forgot-password-pending";
    }

    @GetMapping("/reset-password")
    String resetPasswordPage(@RequestParam("token") String token, Model model) {
        // Pre-fill the token in the form object so it can be put into a hidden input
        model.addAttribute("resetPasswordForm", new ResetPasswordForm(token, "", ""));
        return "reset-password";
    }

    // 2. Handle the Form Submission
    @PostMapping("/reset-password")
    String performResetPassword(
            @ModelAttribute ResetPasswordForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (!form.password().equals(form.confirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
            return "reset-password";
        }

        try {
            var backendRequest = new ResetPasswordBackendRequest(form.token(), form.password());

            apiGatewayClient.post()
                    .uri("/users/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(backendRequest)
                    .retrieve()
                    .toBodilessEntity();

            return "redirect:/login?reset=true";

        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "Failed to reset password. The link may be invalid or expired.");
            return "reset-password";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred.");
            return "reset-password";
        }
    }

    private void handleBackendError(HttpClientErrorException e, BindingResult bindingResult, Model model) {
        try {
            // 1. Parse the JSON body from the exception
            var apiError = e.getResponseBodyAs(ApiError.class);

            if (apiError != null) {
                // Case A: Validation Errors (400)
                if (apiError.validationErrors() != null && !apiError.validationErrors().isEmpty()) {
                    for (var error : apiError.validationErrors()) {
                        // Map backend field error to UI BindingResult
                        // This makes th:errors="*{field}" work in Thymeleaf!
                        bindingResult.rejectValue(error.field(), "backend", error.message());
                    }
                }
                // Case B: Logic Errors (e.g., 409 User Already Exists)
                else {
                    // Show the 'detail' message from the backend in the global alert
                    model.addAttribute("error", apiError.detail());
                }
            } else {
                // Fallback if JSON parsing failed
                model.addAttribute("error", "Server returned error: " + e.getStatusCode());
            }
        } catch (Exception parseEx) {
            model.addAttribute("error", "Could not parse error response from server.");
        }
    }

    private void setJwtCookie(HttpServletResponse response, String name, String value, int maxAge) {
        var cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}