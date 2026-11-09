import { mount, flushPromises } from "@vue/test-utils";
import { describe, it, expect, vi, beforeEach } from "vitest";
import PrimeVue from "primevue/config";
import ToastService from "primevue/toastservice";
vi.mock("../../composables/useConfirmAction", () => ({
  useConfirmAction: () => async () => true,
}));
import ClientEditor from "./ClientEditor.vue";
import { clientsApi, type Client } from "./api";
import { ApiError } from "../../services/http";

beforeEach(() => {
  // APIs de layout usadas por PrimeVue que jsdom no implementa.
  vi.stubGlobal("ResizeObserver", class {
    observe() {}
    unobserve() {}
    disconnect() {}
  });
  vi.stubGlobal("matchMedia", (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(() => true),
  }));
  vi.spyOn(clientsApi, "countries").mockResolvedValue([{ id: 19, name: "Argentina", countryId: null }]);
  vi.spyOn(clientsApi, "provinces").mockResolvedValue([{ id: 83, name: "Mendoza", countryId: 19 }]);
});

const client: Client = {
  removed: false,
  id: 7,
  firstname: "Ana",
  lastname: "Pérez",
  email: "ana@example.test",
  phone: "123456",
  cuil: "20123456789",
  username: "ana",
  dni: "12345678",
  legalName: null,
  tradeName: null,
  externalCode: null,
  portalUserMissing: false,
};
function editor() {
  return mount(ClientEditor, {
    props: { client: { ...client } },
    global: { plugins: [[PrimeVue, { unstyled: true }], ToastService] },
  });
}

describe("Edición de clientes", () => {
  it("editar y cancelar no modifica el cliente original ni pierde datos de contacto", async () => {
    const wrapper = editor();

    await wrapper.get("#firstname").setValue("Otro nombre");
    await wrapper.findAll("button")[0]!.trigger("click");
    await flushPromises();
    expect(wrapper.emitted("cancel")).toHaveLength(1);
    expect(wrapper.props("client")!.firstname).toBe("Ana");
    expect(wrapper.props("client")!.email).toBe("ana@example.test");
  });

  it("guarda todos los datos de la ficha y conserva teléfono, correo y CUIL", async () => {
    const save = vi.spyOn(clientsApi, "save").mockResolvedValue(client);
    const wrapper = editor();
    await wrapper.get("#lastname").setValue("Gómez");
    await wrapper.get("#street").setValue("Calle de prueba");
    await wrapper.get("#observations").setValue("Recibir por la tarde");
    await wrapper.get("form").trigger("submit");
    await flushPromises();
    expect(save).toHaveBeenCalledWith(
      {
        firstname: "Ana",
        lastname: "Gómez",
        email: "ana@example.test",
        phone: "123456",
        cuil: "20123456789",
        dni: "12345678",
        legalName: "",
        tradeName: "",
        externalCode: "",
        street: "Calle de prueba",
        streetNumber: "",
        addressComplement: "",
        neighborhood: "",
        city: "",
        postalCode: "",
        observations: "Recibir por la tarde",
        countryId: 0,
        provinceId: 0,
      },
      7,
    );
    expect(wrapper.emitted("saved")).toHaveLength(1);
  });

  it("muestra errores del servidor y conserva el borrador para corregirlo", async () => {
    vi.spyOn(clientsApi, "save").mockRejectedValue(
      new ApiError(400, "Revisá los campos.", { email: "Correo inválido" }),
    );
    const wrapper = editor();
    await wrapper.get("form").trigger("submit");
    await flushPromises();
    expect(wrapper.text()).toContain("Correo inválido");
    expect((wrapper.get("#phone").element as HTMLInputElement).value).toBe(
      "123456",
    );
    expect(wrapper.emitted("saved")).toBeUndefined();
  });
});
