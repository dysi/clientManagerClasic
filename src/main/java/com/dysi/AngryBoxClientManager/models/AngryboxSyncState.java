package com.dysi.AngryBoxClientManager.models;

import java.time.Instant;
import javax.persistence.*;

/** Cursor por origen; su bloqueo serializa lotes sin mantener conexiones HTTP abiertas. */
@Entity
@Table(name = "ANGRYBOX_SYNC_STATE")
public class AngryboxSyncState extends AuditableEntity {
    @Id
    @Column(name = "COMPANY_ID")
    private Long companyId;
    @Column(name = "LAST_DATA_AT")
    private Instant lastDataAt;

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Instant getLastDataAt() {
        return lastDataAt;
    }

    public void setLastDataAt(Instant lastDataAt) {
        this.lastDataAt = lastDataAt;
    }
}
