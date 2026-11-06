package com.dysi.AngryBoxClientManager.security;

import com.dysi.AngryBoxClientManager.servicesImpl.PortalAccessService;

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

/** Revalida cuentas y vínculos sin promover un principal restringido a administrador. */
public class WebSessionUserFilter extends OncePerRequestFilter {
    private final UserDetailsService users;
    private final PortalAccessService portal;
    public WebSessionUserFilter(UserDetailsService users,
            PortalAccessService portal) {
        this.users = users;
        this.portal = portal;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                if (authentication.getPrincipal() instanceof PortalPrincipal) {
                    PortalPrincipal principal = portal.revalidate((PortalPrincipal) authentication.getPrincipal());
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
                } else {
                    UserDetails user = users.loadUserByUsername(authentication.getName());
                    boolean passwordUnchanged = !(authentication.getPrincipal() instanceof JwtUser) || !(user instanceof JwtUser)
                            || java.util.Objects.equals(((JwtUser) authentication.getPrincipal()).getLastPasswordResetDate(), ((JwtUser) user).getLastPasswordResetDate());
                    boolean sameIdentity = !(authentication.getPrincipal() instanceof JwtUser) || !(user instanceof JwtUser)
                            || java.util.Objects.equals(((JwtUser) authentication.getPrincipal()).getId(), ((JwtUser) user).getId());
                    if (sameIdentity && passwordUnchanged && user.isEnabled() && user.isAccountNonLocked() && user.isAccountNonExpired()
                            && (!(user instanceof JwtUser) || portal.isActiveUser((JwtUser) user))) {
                        if (user instanceof JwtUser && user.getAuthorities().stream()
                                .noneMatch(role -> role.getAuthority().equals("ROLE_ADMIN"))) {
                            user = portal.authenticatedLogin((JwtUser) user);
                        }
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                    } else { invalidate(request); }
                }
            } catch (AuthenticationException | org.springframework.web.server.ResponseStatusException error) {
                invalidate(request);
            }
        }
        chain.doFilter(request, response);
    }
    private void invalidate(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) request.getSession(false).invalidate();
    }
}
