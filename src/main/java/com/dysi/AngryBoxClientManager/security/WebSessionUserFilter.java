package com.dysi.AngryBoxClientManager.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

/** Account disabling and role changes take effect on existing browser sessions. */
public class WebSessionUserFilter extends OncePerRequestFilter {
    private final UserDetailsService users;
    public WebSessionUserFilter(UserDetailsService users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                UserDetails user = users.loadUserByUsername(authentication.getName());
                boolean passwordUnchanged = !(authentication.getPrincipal() instanceof JwtUser) || !(user instanceof JwtUser)
                        || java.util.Objects.equals(((JwtUser) authentication.getPrincipal()).getLastPasswordResetDate(), ((JwtUser) user).getLastPasswordResetDate());
                if (passwordUnchanged && user.isEnabled() && user.isAccountNonLocked() && user.isAccountNonExpired()) {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                } else { invalidate(request); }
            } catch (AuthenticationException error) { invalidate(request); }
        }
        chain.doFilter(request, response);
    }
    private void invalidate(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) request.getSession(false).invalidate();
    }
}
