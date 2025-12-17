package pl.bpiatek.linkshortenerui.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
class SessionConfig {

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> disableSessionFilter() {
        var registration = new FilterRegistrationBean<OncePerRequestFilter>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {
                request.getSession(false);
                filterChain.doFilter(request, response);
            }
        });
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }
}
