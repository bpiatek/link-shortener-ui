package pl.bpiatek.linkshortenerui.api;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import pl.bpiatek.linkshortenerui.dto.LinkDto;
import pl.bpiatek.linkshortenerui.dto.PageResponse;

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
}
