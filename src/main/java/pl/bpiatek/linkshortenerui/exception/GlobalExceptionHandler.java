package pl.bpiatek.linkshortenerui.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import pl.bpiatek.linkshortenerui.api.TokenExtractor;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper objectMapper;
    private final TokenExtractor tokenExtractor;

    public GlobalExceptionHandler(ObjectMapper objectMapper, TokenExtractor tokenExtractor) {
        this.objectMapper = objectMapper;
        this.tokenExtractor = tokenExtractor;
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public String handleHttpClientError(HttpClientErrorException ex) {

        log.warn("HttpClientErrorException Error: {}", ex.getMessage());
        if (ex.getStatusCode() == UNAUTHORIZED) {
            log.info("GlobalExceptionHandler caught 401 - Redirecting to login");
            return "redirect:/login";
        }

        throw ex;
    }

    @ExceptionHandler(RestClientResponseException.class)
    public String handleBackendError(RestClientResponseException ex, Model model) {
        log.error("Backend API Error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());

        model.addAttribute("status", ex.getStatusCode().value());

        if (ex.getStatusCode().value() == 405) {
            model.addAttribute("error", "Operation not allowed (Method Not Allowed).");
        } else if (ex.getStatusCode().is5xxServerError()) {
            model.addAttribute("error", "Our servers are having trouble. Please try again later.");
        } else {
            model.addAttribute("error", "The request could not be processed.");
        }

        model.addAttribute("status", ex.getStatusCode().value());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralError(Exception ex, Model model) {
        log.error("Unexpected UI Error", ex);

        model.addAttribute("status", 500);
        model.addAttribute("error", "An unexpected error occurred in the application.");

        return "error";
    }

    @ModelAttribute("userEmail")
    public String populateUserEmail(@CookieValue(value = "jwt", required = false) String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return null;
        }

        return tokenExtractor.extractEmail(jwt);
    }
}