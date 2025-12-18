package pl.bpiatek.linkshortenerui.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
class TokenExtractor {

    private final ObjectMapper objectMapper;

    TokenExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extractEmail(String jwt) {
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
