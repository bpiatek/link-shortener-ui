package pl.bpiatek.linkshortenerui.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pl.bpiatek.linkshortenerui.api.TokenExtractor;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final TokenExtractor tokenExtractor;

    public GlobalExceptionHandler(TokenExtractor tokenExtractor) {
        this.tokenExtractor = tokenExtractor;
    }

    /**
     * Handles ALL errors coming from the Backend API (RestTemplate/RestClient).
     * This covers 4xx (HttpClientErrorException) and 5xx (HttpServerErrorException).
     */
    @ExceptionHandler(RestClientResponseException.class)
    public String handleBackendError(RestClientResponseException ex, Model model) {

        // 1. Handle 401 Unauthorized (Token expired/invalid)
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.info("Backend returned 401 - Redirecting to login");
            return "redirect:/login";
        }

        // 2. Handle 404 from Backend (Optional: if backend 404 means "Link not found")
        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            return "404";
        }

        // 3. Handle other errors (400, 403, 405, 500, 503, etc.)
        log.error("Backend API Error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());

        model.addAttribute("status", ex.getStatusCode().value());

        if (ex.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED) {
            model.addAttribute("error", "Operation not allowed (Method Not Allowed).");
        } else if (ex.getStatusCode().is5xxServerError()) {
            model.addAttribute("error", "Our servers are having trouble. Please try again later.");
        } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            model.addAttribute("error", "You do not have permission to perform this action.");
        } else {
            model.addAttribute("error", "The request could not be processed.");
        }

        return "error";
    }

    /**
     * Handles 404 errors for UI routes (e.g., user types /does-not-exist).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoResourceFoundException ex) {
        return "404";
    }

    /**
     * Catch-all for unexpected NullPointers, Parsing errors, etc.
     */
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