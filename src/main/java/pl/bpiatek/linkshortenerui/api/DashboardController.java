package pl.bpiatek.linkshortenerui.api;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.bpiatek.linkshortenerui.dto.CreateLinkRequest;
import pl.bpiatek.linkshortenerui.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerui.dto.LinkDto;
import pl.bpiatek.linkshortenerui.dto.PageResponse;
import pl.bpiatek.linkshortenerui.dto.UpdateLinkRequest;
import pl.bpiatek.linkshortenerui.exception.BackendErrorMapper;

@Controller
class DashboardController {

    private final BackendApiService backendApi;
    private final RestClient restClient;
    private final BackendErrorMapper errorMapper;

    DashboardController(BackendApiService backendApi, RestClient restClient, BackendErrorMapper errorMapper) {
        this.backendApi = backendApi;
        this.restClient = restClient;
        this.errorMapper = errorMapper;
    }

    @GetMapping("/dashboard")
    String dashboard(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created_at,desc") String sort
    ) {
        var responseType = new ParameterizedTypeReference<PageResponse<LinkDto>>() {};

        PageResponse<LinkDto> linkPage = backendApi.execute(jwt ->
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/dashboard/links")
                                .queryParam("page", page)
                                .queryParam("size", size)
                                .queryParam("sort", sort)
                                .build())
                        .header("Authorization", "Bearer " + jwt)
                        .retrieve()
                        .body(responseType)
        );

        model.addAttribute("page", linkPage);
        return "dashboard";
    }

    @PostMapping("/dashboard/links")
    String createLink(
            @RequestParam String longUrl,
            @RequestParam(required = false) String shortUrl,
            @RequestParam(defaultValue = "false") boolean isActive,
            @RequestParam(required = false) String title,
            RedirectAttributes redirectAttributes // <--- Added for Flash Attributes
    ) {
        var request = new CreateLinkRequest(longUrl, shortUrl, isActive, title);

        try {
            backendApi.execute(userJwt -> restClient.post()
                    .uri("/links")
                    .header("Authorization", "Bearer " + userJwt)
                    .body(request)
                    .retrieve()
                    .body(CreateLinkResponse.class)
            );

            redirectAttributes.addFlashAttribute("success", "Link created successfully!");

        } catch (RestClientResponseException e) {
            errorMapper.map(e, redirectAttributes);

            redirectAttributes.addFlashAttribute("longUrl", longUrl);
            redirectAttributes.addFlashAttribute("shortUrl", shortUrl);
            redirectAttributes.addFlashAttribute("title", title);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred.");
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard/links/{id}/edit")
    String editLinkPage(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            LinkDto link = backendApi.execute(jwt -> restClient.get()
                    .uri("/links/{id}", id)
                    .header("Authorization", "Bearer " + jwt)
                    .retrieve()
                    .body(LinkDto.class)
            );

            var form = new UpdateLinkRequest(link.longUrl(), link.isActive(), link.title());

            model.addAttribute("linkId", id);
            model.addAttribute("updateLinkRequest", form);
            model.addAttribute("shortUrl", link.shortUrl());

            return "link-edit";

        } catch (HttpClientErrorException.NotFound e) {
            redirectAttributes.addFlashAttribute("error", "Link not found.");
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/dashboard/links/{id}/edit")
    String updateLink(
            @PathVariable Long id,
            @ModelAttribute("updateLinkRequest") UpdateLinkRequest request,
            @RequestParam(defaultValue = "false") boolean isActive,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        var finalRequest = new UpdateLinkRequest(request.longUrl(), isActive, request.title());

        try {
            backendApi.execute(jwt -> restClient.patch()
                    .uri("/links/{id}", id)
                    .header("Authorization", "Bearer " + jwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(finalRequest)
                    .retrieve()
                    .toBodilessEntity()
            );

            redirectAttributes.addFlashAttribute("success", "Link updated successfully!");
            return "redirect:/dashboard";

        } catch (RestClientResponseException e) {
            // --- ERROR HANDLING ---

            // 2. Map errors to BindingResult (fields) or Model (global)
            errorMapper.map(e, bindingResult, model);

            // 3. Re-populate data required by the view (shortUrl)
            // We need to fetch the link again because the POST request doesn't contain the shortUrl
            try {
                LinkDto link = backendApi.execute(jwt -> restClient.get()
                        .uri("/links/{id}", id)
                        .header("Authorization", "Bearer " + jwt)
                        .retrieve()
                        .body(LinkDto.class)
                );
                model.addAttribute("shortUrl", link.shortUrl());
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("error", "An error occurred.");
                return "redirect:/dashboard";
            }

            model.addAttribute("linkId", id);

            return "link-edit";
        }
    }

    @PostMapping("/dashboard/links/{id}/delete")
    String deleteLink(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            backendApi.execute(jwt -> restClient.delete()
                    .uri("/links/{id}", id)
                    .header("Authorization", "Bearer " + jwt)
                    .retrieve()
                    .toBodilessEntity()
            );
            redirectAttributes.addFlashAttribute("success", "Link deleted successfully.");

        } catch (HttpClientErrorException e) {
            errorMapper.map(e, redirectAttributes);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete link.");
        }
        return "redirect:/dashboard";
    }
}