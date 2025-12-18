package pl.bpiatek.linkshortenerui.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SessionConfig {

    @Bean
    public Filter sessionDisablingFilter() {
        return (request, response, chain) -> {
            if (request instanceof HttpServletRequest req) {
                req.getSession(false);
            }
            chain.doFilter(request, response);
        };
    }
}
