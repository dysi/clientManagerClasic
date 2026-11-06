export interface Bank {
  id: number;
  name: string;
  active: boolean;
}
export type BankInput = Omit<Bank, "id">;
export interface BankAccount {
  id: number;
  bankId: number;
  bankName: string;
  holder: string;
  cbu: string;
  alias: string | null;
  active: boolean;
  bankActive: boolean;
  available: boolean;
}
export interface BankAccountInput {
  bankId: number | null;
  holder: string;
  cbu: string;
  alias: string;
  active: boolean;
}
export const parameterStates = [
  { label: "Todos", value: "ALL" },
  { label: "Activos", value: "ACTIVE" },
  { label: "Inactivos", value: "INACTIVE" },
];
