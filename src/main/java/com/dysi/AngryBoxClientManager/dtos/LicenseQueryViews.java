package com.dysi.AngryBoxClientManager.dtos;

import com.dysi.AngryBoxClientManager.models.LicenseQueryLog;
import java.time.Instant;
import java.time.LocalDate;
import javax.validation.constraints.Size;

public final class LicenseQueryViews {
    private LicenseQueryViews() { }

    /** Metadatos opcionales y declarados por el servidor AngryBox, nunca identidad verificada. */
    public static class Metadata {
        @Size(max = 100) public String installationId;
        @Size(max = 100) public String processId;
        @Size(max = 255) public String hostname;
        @Size(max = 100) public String appVersion;
        @Size(max = 100) public String operatingSystem;
    }

    public static class Filter {
        @Size(max = 150) public String keyword;
        public Long companyId;
        public Long licenseId;
        @Size(max = 64) public String ip;
        @javax.validation.constraints.Pattern(regexp = "ALL|SUCCESS|ERROR") public String result = "ALL";
        @javax.validation.constraints.Pattern(regexp = "ALL|v1|v2") public String version = "ALL";
        @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
        public LocalDate from;
        @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
        public LocalDate to;
    }

    public static class Entry {
        public Long id, licenseId, companyId, lockDateEpochMs;
        public Instant receivedAt;
        public String companyName, authenticatedUsername, requestedUsername, remoteIp, userAgent, httpMethod, apiVersion;
        public String installationId, processId, hostname, appVersion, operatingSystem;
        public int httpStatus;
        public long durationMs;
        public Boolean forcedLock;
        public Entry(LicenseQueryLog log) {
            id = log.getId(); licenseId = log.getLicenseId(); companyId = log.getCompanyId();
            receivedAt = log.getReceivedAt(); companyName = log.getCompanyName();
            authenticatedUsername = log.getAuthenticatedUsername(); requestedUsername = log.getRequestedUsername();
            remoteIp = log.getRemoteIp(); userAgent = log.getUserAgent(); httpMethod = log.getHttpMethod();
            apiVersion = log.getApiVersion(); httpStatus = log.getHttpStatus(); durationMs = log.getDurationMs();
            forcedLock = log.getForcedLock(); lockDateEpochMs = log.getLockDateEpochMs();
            installationId = log.getInstallationId(); processId = log.getProcessId(); hostname = log.getHostname();
            appVersion = log.getAppVersion(); operatingSystem = log.getOperatingSystem();
        }
    }

    public static class Summary {
        public long total, errors, distinctIps, distinctInstallations;
        public Instant firstSeen, lastSeen;
        public int retentionDays;
        public String timeZone;
    }

    public static class Origin {
        public Long companyId;
        public String remoteIp, authenticatedUsername, installationId, hostname;
        public long queries, errors;
        public Instant firstSeen, lastSeen;
    }
}
