package pl.bpiatek.linkshortenerui.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
public class BackendErrorMapper {

    private final static Logger log = LoggerFactory.getLogger(BackendErrorMapper.class);

    /**
     * Use this for standard forms (Login, Register) where you return a View name.
     * It maps validation errors to specific input fields if BindingResult is provided.
     */
    public void map(RestClientResponseException e, BindingResult bindingResult, Model model) {
        log.info("Backend API Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

        var apiError = parseError(e);

        if (apiError == null) {
            model.addAttribute("error", "Server returned error: " + e.getStatusCode());
            return;
        }

        // 1. Handle Validation Errors (400)
        if (hasValidationErrors(apiError)) {
            if (bindingResult != null) {
                for (var error : apiError.validationErrors()) {
                    // "backend" is the error code.
                    // This allows th:errors="*{field}" to work in Thymeleaf
                    bindingResult.rejectValue(error.field(), "backend", error.message());
                }
            } else {
                // Fallback if no BindingResult: show first error globally
                model.addAttribute("error", apiError.validationErrors().get(0).message());
            }
        }
        // 2. Handle Logic Errors (409 Conflict, 404 Not Found, etc.)
        else if (apiError.detail() != null) {
            model.addAttribute("error", apiError.detail());
        }
        // 3. Fallback
        else {
            model.addAttribute("error", "An unexpected error occurred.");
        }
    }

    /**
     * Overload for simple pages without form binding (e.g. Verify Email page).
     */
    public void map(RestClientResponseException e, Model model) {
        log.info("Backend API Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        map(e, null, model);
    }

    /**
     * Use this for actions that Redirect (Create Link, Update Link, Delete).
     * It puts the error into FlashAttributes so it survives the redirect.
     */
    public void map(RestClientResponseException e, RedirectAttributes redirectAttributes) {
        log.info("Backend API Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

        var apiError = parseError(e);

        if (apiError == null) {
            redirectAttributes.addFlashAttribute("error", "Request failed with status: " + e.getStatusCode());
            return;
        }

        // For redirects, we can't easily map to specific fields on the next page.
        // We pick the most relevant message to show in the Global Alert.
        if (hasValidationErrors(apiError)) {
            // Show the message of the first validation error found
            String firstMsg = apiError.validationErrors().get(0).message();
            redirectAttributes.addFlashAttribute("error", firstMsg);
        } else if (apiError.detail() != null) {
            redirectAttributes.addFlashAttribute("error", apiError.detail());
        } else {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred.");
        }
    }

    private ApiError parseError(RestClientResponseException e) {
        log.info("Backend API Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        try {
            return e.getResponseBodyAs(ApiError.class);
        } catch (Exception ex) {
            log.info(ex.getMessage());
            // Failed to parse JSON (maybe backend is down or returned HTML)
            return null;
        }
    }

    private boolean hasValidationErrors(ApiError error) {
        return error.validationErrors() != null && !error.validationErrors().isEmpty();
    }
}