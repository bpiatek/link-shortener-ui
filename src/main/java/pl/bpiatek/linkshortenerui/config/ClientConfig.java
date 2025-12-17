package pl.bpiatek.linkshortenerui.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.io.IOException;

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

    @Bean
    public Filter sessionDisablingFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                if (request instanceof HttpServletRequest req) {
                    req.getSession(false);
                }
                chain.doFilter(request, response);
            }
        };
    }
}
