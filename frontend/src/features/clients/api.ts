import { request } from "../../services/http";

export interface AccountBalance {
  id: number;
  clientId: number;
  amount: number;
  currency: string;
  synchronizedAt: string;
}

export interface Client {
  street?: string | null;
  streetNumber?: string | null;
  addressComplement?: string | null;
  neighborhood?: string | null;
  city?: string | null;
  postalCode?: string | null;
  observations?: string | null;
  countryId?: number | null;
  provinceId?: number | null;
  countryName?: string | null;
  provinceName?: string | null;
  removed: boolean;
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
  userCount: number;
}
export type ClientInput = Pick<
  Client,
  "street" | "streetNumber" | "addressComplement" | "neighborhood" | "city" | "postalCode" | "observations" | "countryId" | "provinceId" | "firstname" | "lastname" | "email" | "phone" | "cuil" | "dni" | "legalName" | "tradeName" | "externalCode"
>;
export interface ClientPage {
  content: Client[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
export function emptyClient(): ClientInput {
  return { street: "", streetNumber: "", addressComplement: "", neighborhood: "", city: "", postalCode: "", observations: "", countryId: 0, provinceId: 0, firstname: "", lastname: "", email: "", phone: "", cuil: "", dni: "", legalName: "", tradeName: "", externalCode: "" };
}
export function clientDraft(client: Client): ClientInput {
  return {
    street: client.street ?? "",
    streetNumber: client.streetNumber ?? "",
    addressComplement: client.addressComplement ?? "",
    neighborhood: client.neighborhood ?? "",
    city: client.city ?? "",
    postalCode: client.postalCode ?? "",
    observations: client.observations ?? "",
    countryId: client.countryId ?? 0,
    provinceId: client.provinceId ?? 0,
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
export interface Place { id: number; name: string; countryId: number | null }
export const clientsApi = {
  countries: () => request<Place[]>("/web/api/clients/countries"),
  provinces: () => request<Place[]>("/web/api/clients/provinces"),
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
