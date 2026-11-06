package com.dysi.AngryBoxClientManager.config;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The bare port is outside Spring MVC when the application has a context path. */
@Configuration
public class PortEntryConfig {
    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> portEntry(
            @Value("${server.servlet.context-path:}") String contextPath) {
        return factory -> {
            if (contextPath.isEmpty() || "/".equals(contextPath)) return;
            factory.addEngineValves(new ValveBase(true) {
                @Override
                public void invoke(Request request, Response response) throws IOException, ServletException {
                    if ("/".equals(request.getRequestURI())
                            && ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))) {
                        response.setHeader("Cache-Control", "no-store");
                        response.sendRedirect(contextPath + "/");
                        return;
                    }
                    getNext().invoke(request, response);
                }
            });
        };
    }
}
