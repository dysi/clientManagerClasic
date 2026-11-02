import { request } from "./http";

export interface LicenseQueryEntry {
  id: number;
  receivedAt: string;
  companyId: number | null;
  licenseId: number | null;
  companyName: string | null;
  authenticatedUsername: string | null;
  requestedUsername: string | null;
  remoteIp: string;
  userAgent: string | null;
  httpMethod: string;
  apiVersion: string;
  httpStatus: number;
  durationMs: number;
  forcedLock: boolean | null;
  lockDateEpochMs: number | null;
  installationId: string | null;
  processId: string | null;
  hostname: string | null;
  appVersion: string | null;
  operatingSystem: string | null;
}

export interface LicenseQuerySummary {
  total: number;
  errors: number;
  distinctIps: number;
  distinctInstallations: number;
  firstSeen: string | null;
  lastSeen: string | null;
  retentionDays: number;
  timeZone: string;
}

export interface LicenseQueryOrigin {
  companyId: number | null;
  remoteIp: string;
  authenticatedUsername: string | null;
  installationId: string | null;
  hostname: string | null;
  queries: number;
  errors: number;
  firstSeen: string;
  lastSeen: string;
}

export const licenseQueryLog = {
  summary: (params: URLSearchParams) => request<LicenseQuerySummary>(`/web/api/license-query-log/summary?${params}`),
  origins: (params: URLSearchParams) => request<LicenseQueryOrigin[]>(`/web/api/license-query-log/origins?${params}`),
};
