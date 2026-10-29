import { request } from "../../services/http";

export interface AccountBalance {
  id: number;
  clientId: number;
  amount: number;
  currency: string;
  synchronizedAt: string;
}

export interface Client {
  accountBalance?: AccountBalance | null;
  id: number;
  firstname: string;
  lastname: string;
  email: string;
  phone: string;
  cuil: string;
  dni: string | null;
  legalName: string | null;
  tradeName: string | null;
  externalCode: string | null;
  portalUserMissing: boolean;
  username: string;
}
export type ClientInput = Pick<
  Client,
  "firstname" | "lastname" | "email" | "phone" | "cuil" | "dni" | "legalName" | "tradeName" | "externalCode"
>;
export interface ClientPage {
  content: Client[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
export function emptyClient(): ClientInput {
  return { firstname: "", lastname: "", email: "", phone: "", cuil: "", dni: "", legalName: "", tradeName: "", externalCode: "" };
}
export function clientDraft(client: Client): ClientInput {
  return {
    firstname: client.firstname ?? "",
    lastname: client.lastname ?? "",
    email: client.email ?? "",
    phone: client.phone ?? "",
    cuil: client.cuil ?? "",
    dni: client.dni ?? "",
    legalName: client.legalName ?? "",
    tradeName: client.tradeName ?? "",
    externalCode: client.externalCode ?? "",
  };
}
export const clientsApi = {
  list(keyword: string, page: number, size: number) {
    return request<ClientPage>(
      `/web/api/clients?${new URLSearchParams({ keyword, page: String(page), size: String(size) })}`,
    );
  },
  detail(id: number) {
    return request<Client>(`/web/api/clients/${id}`);
  },
  save(input: ClientInput, id?: number) {
    return request<Client>(`/web/api/clients${id ? `/${id}` : ""}`, {
      method: id ? "PUT" : "POST",
      body: JSON.stringify(input),
    });
  },
};
