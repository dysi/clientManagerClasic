package com.dysi.AngryBoxClientManager.config;

import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Browser traffic uses a session and CSRF; Bearer clients keep their existing chain. */
@Configuration
@Order(1)
public class WebSessionSecurityConfig extends WebSecurityConfigurerAdapter {
    private final org.springframework.security.core.userdetails.UserDetailsService users;
    public WebSessionSecurityConfig(org.springframework.security.core.userdetails.UserDetailsService users) {
        this.users = users;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        RequestMatcher browser = request -> {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            return path.equals("/") || path.equals("/ui") || path.startsWith("/ui/")
                    || path.startsWith("/web/") || path.startsWith("/index/")
                    || path.startsWith("/users/")
                    || (path.startsWith("/api/") && request.getHeader("Authorization") == null);
        };
        http.requestMatcher(browser)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).and()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/", "/ui", "/ui/**", "/users/login", "/web/session/csrf").permitAll()
                .antMatchers(HttpMethod.POST, "/web/session/login").permitAll()
                .antMatchers("/web/api/**", "/index/**", "/users/create/**", "/users/edit/**").hasRole("ADMIN")
                .anyRequest().authenticated().and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, error) -> {
                    String path = request.getRequestURI().substring(request.getContextPath().length());
                    if (path.startsWith("/web/") || path.startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"message\":\"Iniciá sesión para continuar.\"}");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/ui/#/login");
                    }
                })
                .accessDeniedHandler((request, response, error) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"No tenés permiso o la protección de la sesión venció. Actualizá la página.\"}");
                }).and()
                .logout()
                .logoutRequestMatcher(new AntPathRequestMatcher("/web/session/logout", "POST"))
                .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    if (request.getHeader("Accept") != null && request.getHeader("Accept").contains("text/html")) {
                        response.sendRedirect(request.getContextPath() + "/ui/#/login");
                    } else {
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    }
                });
        // Default session-backed CSRF repository is deliberately enabled.
        http.addFilterAfter(new com.dysi.AngryBoxClientManager.security.WebSessionUserFilter(users),
                org.springframework.security.web.context.SecurityContextPersistenceFilter.class);
        http.headers().addHeaderWriter((request, response) -> {
            if (request.getRequestURI().substring(request.getContextPath().length()).startsWith("/ui")) {
                response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'");
            }
        });

    }
}
