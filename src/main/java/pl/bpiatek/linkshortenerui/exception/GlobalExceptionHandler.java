package pl.bpiatek.linkshortenerui.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Base64;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    @ExceptionHandler(HttpClientErrorException.class)
    public String handleHttpClientError(HttpClientErrorException ex) {

        if (ex.getStatusCode() == UNAUTHORIZED) {
            log.info("GlobalExceptionHandler caught 401 - Redirecting to login");
            return "redirect:/login";
        }

        throw ex;
    }

    @ModelAttribute("userEmail")
    public String populateUserEmail(@CookieValue(value = "jwt", required = false) String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return null;
        }

        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));

            JsonNode claims = objectMapper.readTree(payloadJson);

            if (claims.has("email")) {
                return claims.get("email").asText();
            }

            return null;
        } catch (Exception e) {
            log.warn("Failed to parse user email from JWT", e);
            return null;
        }
    }
}