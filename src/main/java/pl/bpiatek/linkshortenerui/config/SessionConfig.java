package pl.bpiatek.linkshortenerui.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SessionConfig {

    @Bean
    public ServletWebServerFactory servletContainer() {
        var factory = new TomcatServletWebServerFactory();
        factory.addContextCustomizers(context -> context.setSessionTimeout(0));
        return factory;
    }
}
