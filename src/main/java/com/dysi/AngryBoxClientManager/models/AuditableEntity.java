package com.dysi.AngryBoxClientManager.models;

import com.dysi.AngryBoxClientManager.audit.AuditEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Calendar;
import javax.persistence.*;

/** Common audit metadata. Historical unknown values remain null until a real event occurs. */
@MappedSuperclass
@EntityListeners(AuditEntityListener.class)
public abstract class AuditableEntity {
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATIONDATE", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Calendar creationDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "MODIFICATIONDATE")
    @JsonIgnore
    private Calendar modificationDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "REMOVEDATE")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Calendar removeDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREATED_BY_ID", updatable = false)
    @JsonIgnore
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MODIFIED_BY_ID")
    @JsonIgnore
    private User modifiedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REMOVED_BY_ID")
    @JsonIgnore
    private User removedBy;

    @Transient private Calendar originalRemoveDate;
    @Transient private User originalRemovedBy;

    public Calendar getCreationDate() { return copy(creationDate); }
    @JsonIgnore public Calendar getModificationDate() { return copy(modificationDate); }
    public Calendar getRemoveDate() { return copy(removeDate); }
    @JsonIgnore public User getCreatedBy() { return createdBy; }
    @JsonIgnore public User getModifiedBy() { return modifiedBy; }
    @JsonIgnore public User getRemovedBy() { return removedBy; }

    /** Repeated removal preserves its first date/actor. Null explicitly reactivates the entity. */
    public void setRemoveDate(Calendar date) {
        if (date == null || removeDate == null) removeDate = copy(date);
    }

    public void markRemoved() { setRemoveDate(Calendar.getInstance()); }
    public void restore() { setRemoveDate(null); }

    /** Owned join-table changes may not trigger PreUpdate unless the owner also becomes dirty. */
    public void markModified() { modificationDate = Calendar.getInstance(); }

    /** Called only by the persistence listener; audit metadata is never accepted from JSON. */
    public void auditCreation(Calendar now, User actor) {
        creationDate = copy(now);
        createdBy = actor;
        auditModification(now, actor);
    }

    /** Removal is a transition; ordinary updates of a removed entity keep its original metadata. */
    public void auditModification(Calendar now, User actor) {
        modificationDate = copy(now);
        modifiedBy = actor;
        if (removeDate == null) {
            removedBy = null;
        } else if (originalRemoveDate == null) {
            removeDate = copy(now);
            removedBy = actor;
        } else {
            removeDate = copy(originalRemoveDate);
            removedBy = originalRemovedBy;
        }
    }

    public void rememberAuditState() {
        originalRemoveDate = copy(removeDate);
        originalRemovedBy = removedBy;
    }

    private static Calendar copy(Calendar value) { return value == null ? null : (Calendar) value.clone(); }
}
