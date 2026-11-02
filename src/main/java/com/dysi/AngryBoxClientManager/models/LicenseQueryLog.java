package com.dysi.AngryBoxClientManager.models;

import java.time.Instant;
import javax.persistence.*;

/** Fotografía de una consulta. Los datos declarados no prueban identidad de una máquina. */
@Entity
@Table(name = "LICENSE_QUERY_LOG", indexes = {
    @Index(name = "ix_license_query_received", columnList = "RECEIVED_AT, ID"),
    @Index(name = "ix_license_query_company", columnList = "COMPANY_ID, RECEIVED_AT"),
    @Index(name = "ix_license_query_license", columnList = "LICENSE_ID, RECEIVED_AT"),
    @Index(name = "ix_license_query_ip", columnList = "REMOTE_IP, RECEIVED_AT")
})
public class LicenseQueryLog extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Column(name = "RECEIVED_AT", nullable = false) private Instant receivedAt;
    // IDs escalares: conservan la evidencia aunque cambien los datos comerciales.
    @Column(name = "LICENSE_ID") private Long licenseId;
    @Column(name = "COMPANY_ID") private Long companyId;
    @Column(length = 255) private String companyName;
    @Column(length = 50) private String authenticatedUsername;
    @Column(length = 150) private String requestedUsername;
    @Column(name = "REMOTE_IP", length = 64, nullable = false) private String remoteIp;
    @Column(length = 300) private String userAgent;
    @Column(length = 16, nullable = false) private String httpMethod;
    @Column(length = 2, nullable = false) private String apiVersion;
    @Column(nullable = false) private int httpStatus;
    @Column(nullable = false) private long durationMs;
    private Boolean forcedLock;
    private Long lockDateEpochMs;
    @Column(length = 100) private String installationId;
    @Column(length = 100) private String processId;
    @Column(length = 255) private String hostname;
    @Column(length = 100) private String appVersion;
    @Column(length = 100) private String operatingSystem;

    public Long getId() { return id; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant value) { receivedAt = value; }
    public Long getLicenseId() { return licenseId; }
    public void setLicenseId(Long value) { licenseId = value; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long value) { companyId = value; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String value) { companyName = value; }
    public String getAuthenticatedUsername() { return authenticatedUsername; }
    public void setAuthenticatedUsername(String value) { authenticatedUsername = value; }
    public String getRequestedUsername() { return requestedUsername; }
    public void setRequestedUsername(String value) { requestedUsername = value; }
    public String getRemoteIp() { return remoteIp; }
    public void setRemoteIp(String value) { remoteIp = value; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String value) { userAgent = value; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String value) { httpMethod = value; }
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String value) { apiVersion = value; }
    public int getHttpStatus() { return httpStatus; }
    public void setHttpStatus(int value) { httpStatus = value; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long value) { durationMs = value; }
    public Boolean getForcedLock() { return forcedLock; }
    public void setForcedLock(Boolean value) { forcedLock = value; }
    public Long getLockDateEpochMs() { return lockDateEpochMs; }
    public void setLockDateEpochMs(Long value) { lockDateEpochMs = value; }
    public String getInstallationId() { return installationId; }
    public void setInstallationId(String value) { installationId = value; }
    public String getProcessId() { return processId; }
    public void setProcessId(String value) { processId = value; }
    public String getHostname() { return hostname; }
    public void setHostname(String value) { hostname = value; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String value) { appVersion = value; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String value) { operatingSystem = value; }
}
