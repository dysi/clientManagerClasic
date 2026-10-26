package com.dysi.AngryBoxClientManager.audit;

import com.dysi.AngryBoxClientManager.models.AuditableEntity;
import com.dysi.AngryBoxClientManager.models.User;
import com.dysi.AngryBoxClientManager.security.JwtUser;
import java.util.Calendar;
import javax.persistence.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Hibernate's Spring bean container injects this listener, including on cascaded writes. */
@Component
public class AuditEntityListener {
    private final ObjectProvider<EntityManager> entityManagers;

    public AuditEntityListener(ObjectProvider<EntityManager> entityManagers) { this.entityManagers = entityManagers; }

    @PrePersist
    public void create(AuditableEntity entity) { entity.auditCreation(Calendar.getInstance(), currentUser()); }

    @PreUpdate
    public void update(AuditableEntity entity) { entity.auditModification(Calendar.getInstance(), currentUser()); }

    @PostLoad @PostPersist @PostUpdate
    public void remember(AuditableEntity entity) { entity.rememberAuditState(); }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) return null;
        Object principal = authentication.getPrincipal();
        Long id;
        if (principal instanceof JwtUser) id = ((JwtUser) principal).getId();
        else if (principal instanceof User) id = ((User) principal).getId();
        else throw new IllegalStateException("An authenticated audit principal must provide a persistent User ID.");
        if (id == null) throw new IllegalStateException("The audit user must already be persisted.");
        // No repository query or cascade: resolving the actor must not trigger a nested flush.
        return entityManagers.getObject().getReference(User.class, id);
    }
}
