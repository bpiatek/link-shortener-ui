package pl.bpiatek.linkshortenerui.api;

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
import pl.bpiatek.linkshortenerui.dto.ResetPasswordBackendRequest;
import pl.bpiatek.linkshortenerui.dto.ResetPasswordRequest;
import pl.bpiatek.linkshortenerui.exception.BackendErrorMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;

@Controller
class PasswordController {

    private final BackendErrorMapper errorMapper;
    private final RestClient apiGatewayClient;


    PasswordController(BackendErrorMapper errorMapper, RestClient apiGatewayClient) {
        this.errorMapper = errorMapper;
        this.apiGatewayClient = apiGatewayClient;
    }

    @GetMapping("/forgot-password")
    String forgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest(""));
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    String performForgotPassword(
            @ModelAttribute ForgotPasswordRequest request,
            Model model) {
        try {
            apiGatewayClient.post()
                    .uri("/users/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            String encodedEmail = URLEncoder.encode(request.email(), UTF_8);
            return "redirect:/forgot-password-pending?email=" + encodedEmail;
        } catch (HttpClientErrorException e) {
            errorMapper.map(e, model);
            return "forgot-password";
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
        model.addAttribute("resetPasswordForm", new ResetPasswordRequest(token, "", ""));
        return "reset-password";
    }

    @PostMapping("/reset-password")
    String performResetPassword(
            @ModelAttribute ResetPasswordRequest form,
            BindingResult bindingResult,
            Model model) {
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
            errorMapper.map(e, bindingResult, model);
            return "reset-password";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred.");
            return "reset-password";
        }
    }
}
