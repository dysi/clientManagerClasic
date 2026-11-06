package com.dysi.AngryBoxClientManager.security;

import com.dysi.AngryBoxClientManager.models.OperationOrigin;
import com.dysi.AngryBoxClientManager.models.User;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Identificación restringida: el usuario asociado nunca aporta sus roles al portal. */
public final class PortalPrincipal extends JwtUser {
    private static final long serialVersionUID = 1L;
    private final Long clientId;
    private final OperationOrigin origin;

    public PortalPrincipal(Long clientId, User user, OperationOrigin origin) {
        super(user.getId(), user.getUsername(), null, null, null, null,
                List.of(new SimpleGrantedAuthority("PORTAL_VIEW"), new SimpleGrantedAuthority("PORTAL_SUBMIT"),
                        new SimpleGrantedAuthority("PORTAL_COMMENT")), true, user.getLastPasswordResetDate());
        this.clientId = clientId;
        this.origin = origin;
    }

    public Long getClientId() { return clientId; }
    public OperationOrigin getOrigin() { return origin; }
}
