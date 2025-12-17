package pl.bpiatek.linkshortenerui.api;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pl.bpiatek.linkshortenerui.dto.CreateLinkRequest;
import pl.bpiatek.linkshortenerui.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerui.dto.LinkDto;
import pl.bpiatek.linkshortenerui.dto.PageResponse;
import pl.bpiatek.linkshortenerui.dto.UpdateLinkRequest;

@Controller
class DashboardController {

    private final BackendApiService backendApi;
    private final RestClient restClient;

    DashboardController(BackendApiService backendApi, RestClient restClient) {
        this.backendApi = backendApi;
        this.restClient = restClient;
    }

    @GetMapping("/dashboard")
    String dashboard(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created_at,desc") String sort // Default sort
    ) {
        // We use a ParameterizedTypeReference to tell Jackson how to unmarshal PageResponse<LinkDto>
        var responseType = new ParameterizedTypeReference<PageResponse<LinkDto>>() {};

        PageResponse<LinkDto> linkPage = backendApi.execute(jwt ->
                restClient.get()
                        // Pass query params to the Gateway
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
            Model model
    ) {
        var request = new CreateLinkRequest(longUrl, shortUrl, isActive, title);

        backendApi.execute(userJwt -> restClient.post()
                .uri("/links")
                .header("Authorization", "Bearer " + userJwt)
                .body(request)
                .retrieve()
                .body(CreateLinkResponse.class)
        );

        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard/links/{id}/edit")
    String editLinkPage(@PathVariable Long id, Model model) {
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
            model.addAttribute("shortUrl", link.shortUrl()); // For display only

            return "link-edit";

        } catch (HttpClientErrorException.NotFound e) {
            return "redirect:/dashboard"; // Or show 404
        }
    }

    @PostMapping("/dashboard/links/{id}/edit")
    String updateLink(
            @PathVariable Long id,
            @ModelAttribute UpdateLinkRequest request,
            @RequestParam(defaultValue = "false") boolean isActive
    ) {
        var finalRequest = new UpdateLinkRequest(request.longUrl(), isActive, request.title());

        backendApi.execute(jwt -> restClient.patch()
                .uri("/links/{id}", id)
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .body(finalRequest)
                .retrieve()
                .toBodilessEntity()
        );

        return "redirect:/dashboard";
    }
}
