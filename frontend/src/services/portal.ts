import { contextPath, request } from "./http";

export interface PortalLicense {
  id: number;
  companyName: string;
  status: "BLOCKED" | "EXPIRED" | "EXPIRING" | "ACTIVE" | "UNKNOWN";
  lockDate: string | null;
  daysUntilLock: number | null;
  forcedLock: boolean;
}
export interface PortalAccount {
  id: number;
  bank: string;
  holder: string;
  cbu: string;
  alias: string | null;
  revision: string;
}
export interface ReceiptSelection { file: File; description: string }
export interface PortalReceipt { id: number; originalName: string; description: string | null; mimeType: string; size: number; uploadedAt: string }
export interface PortalPaymentDetail {
  payment: PortalPayment; licenseId: number | null; contactPhone: string; method: string;
  destinationBank: string; destinationHolder: string; destinationCbu: string; destinationAlias: string | null;
  withoutReceipt: boolean; noReceiptReason: string | null;
}
export function receiptUrl(paymentId: number, attachmentId: number, download = false) {
  return contextPath + `/web/portal/payments/${paymentId}/attachments/${attachmentId}?download=${download}`;
}
export function appendReceipts(id: number, uploadKey: string, receipts: ReceiptSelection[]) {
  const body = new FormData();
  body.append("data", new Blob([JSON.stringify({ uploadKey, descriptions: receipts.map(item => item.description.trim() || null) })], { type: "application/json" }));
  for (const item of receipts) body.append("receipts", item.file);
  return request<{ saved: boolean }>(`/web/portal/payments/${id}/attachments`, { method: "POST", body });
}
export interface PortalPayment {
  commentCount: number;
  receiptCount: number;
  id: number;
  amount: number;
  currency: string;
  status: "PENDIENTE" | "APROBADO" | "RECHAZADO";
  issuedAt: string;
  companyName: string | null;
  rejectionReason: string | null;
}
export interface PortalComment { id: number; text: string; occurredAt: string }
export interface PortalOverview {
  sensitiveAccess: boolean;
  client: { firstname: string | null; lastname: string | null; email: string | null; phone: string | null; dni: string | null; cuil: string | null };
  name: string;
  state: "SIN_LICENCIAS" | "CON_RESTRICCIONES" | "REVISAR_DATOS" | "POR_VENCER" | "VIGENTE" | null;
  balance: null | { amount: number; currency: string; state: "DEUDA" | "A_FAVOR" | "SIN_DEUDA"; synchronizedAt: string };
  licenses: { total: number; blocked: number; expired: number; expiring: number; unknown: number } | null;
}
export interface PaymentFormContext { sensitiveAccess: boolean; accounts: PortalAccount[]; suggestedAmount: number | null; contactPhone: string | null; maxReceipts: number; maxFileBytes: number }
export interface PortalSubmission {
  bankAccountId: number;
  accountRevision: string;
  licenseId: number | null;
  amount: number;
  issuedAt: string;
  contactPhone: string;
  method: "TRANSFERENCIA";
  withoutReceipt: boolean;
  noReceiptReason: string | null;
  comment: string | null;
  submissionKey: string;
  descriptions: (string | null)[];
}
export function submitPayment(data: PortalSubmission, receipts: File[]) {
  const body = new FormData();
  body.append("data", new Blob([JSON.stringify(data)], { type: "application/json" }));
  for (const file of receipts) body.append("receipts", file);
  return request<PortalPayment>("/web/portal/payments", { method: "POST", body });
}
export const overviewLabels: Record<NonNullable<PortalOverview["state"]>, string> = {
  SIN_LICENCIAS: "Sin licencias registradas", CON_RESTRICCIONES: "Tenés licencias bloqueadas o vencidas",
  REVISAR_DATOS: "Hay datos de vigencia por revisar", POR_VENCER: "Tenés licencias por vencer", VIGENTE: "Tus licencias están vigentes",
};
