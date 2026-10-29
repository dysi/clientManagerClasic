package com.dysi.AngryBoxClientManager.models;

import java.time.Instant;
import javax.persistence.*;

/** La respuesta y los saldos se confirman juntos; el UUID no puede reutilizarse con otros datos. */
@Entity
@Table(name = "ANGRYBOX_SYNC_BATCH", uniqueConstraints = @UniqueConstraint(
        name = "uk_angrybox_sync_company_uuid", columnNames = {"COMPANY_ID", "SYNC_UUID"}))
public class AngryboxSyncBatch extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;
    @Column(name = "COMPANY_ID", nullable = false)
    private Long companyId;
    @Column(name = "SYNC_UUID", nullable = false, length = 36)
    private String syncUuid;
    @Column(name = "REQUEST_HASH", nullable = false, length = 64)
    private String requestHash;
    @Column(name = "DATA_AT", nullable = false)
    private Instant dataAt;
    @Column(name = "MATCHING_MODE", nullable = false, length = 32)
    private String matchingMode;
    @Column(name = "CODE_TARGET", nullable = false, length = 32)
    private String codeTarget;
    @Lob
    @Column(name = "RESPONSE_JSON", nullable = false, columnDefinition = "LONGTEXT")
    private String responseJson;

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getSyncUuid() {
        return syncUuid;
    }

    public void setSyncUuid(String syncUuid) {
        this.syncUuid = syncUuid;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public Instant getDataAt() {
        return dataAt;
    }

    public void setDataAt(Instant dataAt) {
        this.dataAt = dataAt;
    }

    public String getMatchingMode() {
        return matchingMode;
    }

    public void setMatchingMode(String matchingMode) {
        this.matchingMode = matchingMode;
    }

    public String getCodeTarget() {
        return codeTarget;
    }

    public void setCodeTarget(String codeTarget) {
        this.codeTarget = codeTarget;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }
}
