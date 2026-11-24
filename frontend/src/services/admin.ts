import { request } from "./http";
export interface PaymentCounts {
  total: number;
  pending: number;
  approved: number;
  rejected: number;
}
export function paymentCounts(params: URLSearchParams) {
  return request<PaymentCounts>(`/web/api/payments/counts?${params}`);
}
export interface License {
  id: number;
  clientId: number;
  clientName: string;
  companyId: number;
  companyName: string;
  description: string;
  initialActivity: string;
  lockDate: string | null;
  daysUntilLock: number | null;
  prePayment: boolean;
  locked: boolean;
  hidden: boolean;
  clientRemoved: boolean;
  status: string;
}
export type LicenseInput = Pick<
  License,
  | "clientId"
  | "companyId"
  | "companyName"
  | "description"
  | "initialActivity"
  | "lockDate"
  | "prePayment"
>;
export type LicenseBatchAction = "LOCK_DATE" | "BLOCK" | "UNBLOCK";
export function updateSelectedLicenses(ids: number[], action: LicenseBatchAction, lockDate: string | null) {
  return request<License[]>("/web/api/licenses/batch", {
    method: "PUT",
    body: JSON.stringify({ ids, action, lockDate: action === "LOCK_DATE" ? lockDate : null }),
  });
}
export interface Payment {
  bankAccountId: number;
  destinationBank: string;
  destinationHolder: string;
  destinationCbu: string;
  destinationAlias: string | null;
  decidedBy: string | null;
  id: number;
  clientId: number;
  clientName: string;
  licenseId: number | null;
  companyName: string | null;
  amount: number;
  currency: "ARS";
  issuedAt: string;
  angryboxSynchronizedAt?: string | null;
  status: "PENDIENTE" | "APROBADO" | "RECHAZADO";
}
export interface User {
  id: number;
  username: string;
  firstname: string;
  lastname: string;
  email: string;
  enabled: boolean;
  roles: string[];
  clientId: number | null;
  clientName: string;
}
export type UserInput = Omit<User, "id" | "clientName"> & { password?: string };
export const roleOptions = [
  { label: "Administrador", value: "ROLE_ADMIN" },
  { label: "Usuario", value: "ROLE_USER" },
  { label: "Vendedor", value: "ROLE_SELLER" },
  { label: "Acceso API", value: "ROLE_API" },
];
export function detail<T>(kind: string, id: number) {
  return request<T>(`/web/api/${kind}/${id}`);
}
export function saveRecord<T>(kind: string, input: unknown, id?: number) {
  return request<T>(`/web/api/${kind}${id ? `/${id}` : ""}`, {
    method: id ? "PUT" : "POST",
    body: JSON.stringify(input),
  });
}
