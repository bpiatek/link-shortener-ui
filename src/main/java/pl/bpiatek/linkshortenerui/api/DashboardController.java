package pl.bpiatek.linkshortenerui.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import pl.bpiatek.linkshortenerui.dto.DashboardLinkResponse;
import pl.bpiatek.linkshortenerui.dto.LinkDto;
import pl.bpiatek.linkshortenerui.dto.PageResponse;
import pl.bpiatek.linkshortenerui.dto.UpdateLinkRequest;
import pl.bpiatek.linkshortenerui.exception.BackendErrorMapper;

import java.util.List;


@Controller
class DashboardController {

    Logger log = LoggerFactory.getLogger(DashboardController.class);

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
            @RequestParam(defaultValue = "created_at,desc") String sort) {
        var responseType = new ParameterizedTypeReference<PageResponse<LinkDto>>() {};

        PageResponse<LinkDto> linkPage = null;

        try {
            linkPage = backendApi.execute(jwt ->
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
        } catch (Exception e) {
            log.error("Failed to fetch dashboard links", e);
            model.addAttribute("error", "Could not load links.");
        }

        if (linkPage == null) {
            linkPage = new PageResponse<>(List.of(), 0, 0, 0, 0);
        } else if (linkPage.content() == null) {
            linkPage = new PageResponse<>(List.of(), linkPage.page(), linkPage.size(), linkPage.totalElements(), linkPage.totalPages());
        }

        model.addAttribute("page", linkPage);

        if (linkPage.content() != null) {
            log.info("Rendering dashboard with {} links. First link: {}",
                    linkPage.content().size(),
                    linkPage.content().isEmpty() ? "None" : linkPage.content().getFirst());
        }

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
            var link = backendApi.execute(jwt -> restClient.get()
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
            errorMapper.map(e, bindingResult, model);
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
        } catch (RestClientResponseException e) {
            errorMapper.map(e, redirectAttributes);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete link.");
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard/links/{linkId}")
    public String linkDetails(
            @PathVariable String linkId,
            Model model
    ) {
        DashboardLinkResponse link;

        try {
            link = backendApi.execute(jwt ->
                    restClient.get()
                            .uri("/dashboard/links/{id}", linkId)
                            .header("Authorization", "Bearer " + jwt)
                            .retrieve()
                            .body(DashboardLinkResponse.class)
            );
        } catch (Exception ex) {
            log.error("Failed to fetch link details for {}", linkId, ex);
            model.addAttribute("error", "Could not load link details.");
            return "redirect:/dashboard";
        }

        model.addAttribute("link", link);
        return "link-details";
    }
}