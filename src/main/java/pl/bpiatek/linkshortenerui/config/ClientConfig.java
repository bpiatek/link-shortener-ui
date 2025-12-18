package pl.bpiatek.linkshortenerui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class ClientConfig {

    @Bean
    RestClient apiGatewayClient(
            @Value("${api.gateway.url}") String gatewayUrl,
            @Value("${api.gateway.host-header}") String hostHeader) {
        return RestClient.builder()
                .baseUrl(gatewayUrl)
                .defaultHeader("Host", hostHeader)
                .build();
    }
}
