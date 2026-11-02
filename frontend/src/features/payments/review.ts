import { contextPath, request } from "../../services/http";
import type { Payment } from "../../services/admin";
export interface Actor { id: number; name: string }
export interface LicenseOption {
  id: number; companyId: number | null; companyName: string; revision: string; forcedLock: boolean; hidden: boolean;
  status: string; today: string; lockDate: string | null; suggestedDate: string;
}
export interface ReviewDetail {
  payment: Payment; version: number; contactPhone: string; method: string;
  destinationBank: string; destinationHolder: string; destinationCbu: string; destinationAlias: string | null;
  origin: string; noReceiptReason: string | null; withoutReceipt: boolean; createdAt: string | null;
  submittedBy: Actor | null; associatedLicense: LicenseOption | null;
}
export interface ReviewComment { id: number; text: string; visibility: "PUBLICO" | "PRIVADO"; origin: string; occurredAt: string; author: Actor | null }
export interface ReviewReceipt { id: number; originalName: string; description: string | null; mimeType: string; size: number; uploadedAt: string; uploadedBy: Actor | null; origin: string }
export interface History { id: number; previousStatus: string | null; newStatus: string; reason: string | null; origin: string; occurredAt: string; responsible: Actor | null }
export interface LicenseEvent { id: number; licenseId: number; companyName: string; actionType: "DESBLOQUEO" | "VENCIMIENTO"; previousValue: string | null; newValue: string; occurredAt: string; responsible: Actor | null }
export interface LicenseAction { licenseId: number; revision: string; unlock: boolean; updateExpiry: boolean; lockDate: string | null; confirmShortening: boolean }
export interface PreviewLicense extends LicenseOption { unlock: boolean; updateExpiry: boolean; shortening: boolean; proposedDate: string | null; daysFromToday: number | null; differenceFromPrevious: number | null; resultingStatus: string }
export interface ApprovalPreview { today: string; token: string; licenses: PreviewLicense[] }
export interface ApprovalLicenses { today: string; suggestedDate: string; licenses: LicenseOption[] }
export function approvalLicenses(id: number) { return request<ApprovalLicenses>(`/web/api/payments/${id}/approval-licenses`); }
export function reviewDetail(id: number) { return request<ReviewDetail>(`/web/api/payments/${id}/detail`); }
export function previewApproval(id: number, version: number, licenses: LicenseAction[]) {
  return request<ApprovalPreview>(`/web/api/payments/${id}/approve-preview`, { method: "POST", body: JSON.stringify({ version, licenses, reviewAllLicenses: true }) });
}
export function approvePayment(id: number, version: number, licenses: LicenseAction[], preview: ApprovalPreview) {
  return request<ReviewDetail>(`/web/api/payments/${id}/approve`, { method: "POST", body: JSON.stringify({ version, licenses, reviewAllLicenses: true, reviewedOn: preview.today, previewToken: preview.token }) });
}
export function rejectPayment(id: number, version: number, reason: string) {
  return request<ReviewDetail>(`/web/api/payments/${id}/reject`, { method: "POST", body: JSON.stringify({ version, reason }) });
}
export function receiptUrl(id: number, attachmentId: number, download = false) { return `${contextPath}/web/api/payments/${id}/attachments/${attachmentId}?download=${download}`; }
export const paymentLabels: Record<Payment["status"], string> = { PENDIENTE: "Pendiente", APROBADO: "Aprobado", RECHAZADO: "Rechazado" };
export function originLabel(value: string) { return value === "PORTAL_DOCUMENTO" ? "Ingreso por documento" : "Acceso autenticado"; }
