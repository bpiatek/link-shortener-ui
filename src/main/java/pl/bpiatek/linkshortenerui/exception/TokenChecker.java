package pl.bpiatek.linkshortenerui.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class TokenChecker {

    private static final Logger log = LoggerFactory.getLogger(TokenChecker.class);

    private final ObjectMapper objectMapper;

    TokenChecker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isTokenExpired(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return true;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payloadJson);

            if (claims.has("exp")) {
                long exp = claims.get("exp").asLong();
                long now = System.currentTimeMillis() / 1000;
                // Return true if expired (with a small 5-second buffer for clock skew)
                return exp < (now - 5);
            }
            // If no exp claim, assume valid or handle accordingly
            return false;
        } catch (Exception e) {
            log.error("Error checking token expiration", e);
            return true; // Treat malformed tokens as expired
        }
    }


    public String extractEmail(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return null;
        }
        try {
            var parts = jwt.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            var payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            var claims = objectMapper.readTree(payloadJson);

            return claims.has("email") ? claims.get("email").asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
