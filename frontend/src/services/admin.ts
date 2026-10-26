import { request } from "./http";
export interface License {
  id: number;
  clientId: number;
  clientName: string;
  companyId: number;
  companyName: string;
  description: string;
  initialActivity: string;
  lockDate: string | null;
  prePayment: boolean;
  locked: boolean;
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
export interface Payment {
  id: number;
  licenseId: number;
  companyName: string;
  amount: number;
  discount: number;
  paymentDate: string;
  expirationDate: string | null;
  paymentRealDate: string | null;
  observation: string;
  removed: boolean;
}
export type PaymentInput = Omit<Payment, "id" | "companyName" | "removed">;
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
