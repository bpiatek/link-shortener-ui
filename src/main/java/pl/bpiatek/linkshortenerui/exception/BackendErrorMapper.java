package pl.bpiatek.linkshortenerui.exception;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
public class BackendErrorMapper {

    /**
     * Use this for standard forms (Login, Register) where you return a View name.
     */
    public void map(HttpClientErrorException e, BindingResult bindingResult, Model model) {
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
        else {
            model.addAttribute("error", apiError.detail());
        }
    }

    public void map(HttpClientErrorException e, Model model) {
        map(e, null, model);
    }

    /**
     * Use this for actions that Redirect (Create Link, Update Link).
     * It puts the error into FlashAttributes.
     */
    public void map(HttpClientErrorException e, RedirectAttributes redirectAttributes) {
        var apiError = parseError(e);

        if (apiError == null) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred.");
            return;
        }

        // For redirects, we can't easily map to specific fields on the next page.
        // We usually just pick the most relevant message to show in the Global Alert.
        if (hasValidationErrors(apiError)) {
            // Show the message of the first validation error found
            String firstMsg = apiError.validationErrors().get(0).message();
            redirectAttributes.addFlashAttribute("error", firstMsg);
        } else {
            redirectAttributes.addFlashAttribute("error", apiError.detail());
        }
    }

    private ApiError parseError(HttpClientErrorException e) {
        try {
            return e.getResponseBodyAs(ApiError.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean hasValidationErrors(ApiError error) {
        return error.validationErrors() != null && !error.validationErrors().isEmpty();
    }
}
