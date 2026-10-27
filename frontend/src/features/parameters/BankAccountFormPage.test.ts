import { mount, flushPromises } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PrimeVue from "primevue/config";
import ToastService from "primevue/toastservice";
import type { BankAccount } from "../../services/parameters";
import { ApiError } from "../../services/http";

const mocks = vi.hoisted(() => ({ detail: vi.fn(), save: vi.fn(), push: vi.fn(), confirm: vi.fn() }));
vi.mock("../../services/admin", () => ({ detail: mocks.detail, saveRecord: mocks.save }));
vi.mock("vue-router", () => ({
  useRoute: () => ({ params: { id: "7" } }),
  useRouter: () => ({ push: mocks.push }),
  onBeforeRouteLeave: () => undefined,
}));
vi.mock("../../composables/useConfirmAction", () => ({ useConfirmAction: () => mocks.confirm }));
import BankAccountFormPage from "./BankAccountFormPage.vue";

const account: BankAccount = {
  id: 7, bankId: 3, bankName: "Banco de prueba", holder: "Titular original",
  cbu: "0000000000000000000000", alias: "original", active: true, bankActive: true, available: true,
};
function form() {
  return mount(BankAccountFormPage, {
    global: {
      plugins: [[PrimeVue, { unstyled: true }], ToastService],
      stubs: { RouterLink: true, EntitySelect: true },
    },
  });
}
beforeEach(() => {
  vi.resetAllMocks();
  mocks.detail.mockResolvedValue(account);
  mocks.save.mockResolvedValue(account);
  mocks.confirm.mockResolvedValue(true);
});
describe("Formulario de cuenta bancaria", () => {
  it("edita un borrador completo sin mutar la respuesta compartida", async () => {
    const wrapper = form();
    try {
      await flushPromises();
      await wrapper.get("#holder").setValue("Nuevo titular");
      await wrapper.get("form").trigger("submit");
      await flushPromises();
      expect(account.holder).toBe("Titular original");
      expect(mocks.save).toHaveBeenCalledWith("bank-accounts", {
        bankId: 3, holder: "Nuevo titular", cbu: account.cbu, alias: "original", active: true,
      }, 7);
      expect(mocks.push).toHaveBeenCalledWith("/parameters/bank-accounts");
    } finally { wrapper.unmount(); }
  });
  it("conserva los datos y muestra el error por campo después de fallar", async () => {
    mocks.save.mockRejectedValue(new ApiError(400, "Revisá los campos.", { cbu: "CBU inválido" }));
    const wrapper = form();
    try {
      await flushPromises();
      await wrapper.get("#cbu").setValue("123");
      await wrapper.get("form").trigger("submit");
      await flushPromises();
      expect(wrapper.text()).toContain("CBU inválido");
      expect(wrapper.get<HTMLInputElement>("#cbu").element.value).toBe("123");
      expect(wrapper.get<HTMLInputElement>("#holder").element.value).toBe("Titular original");
      expect(mocks.push).not.toHaveBeenCalled();
    } finally { wrapper.unmount(); }
  });
  it("evita enviar dos veces mientras hay una escritura pendiente", async () => {
    let finish: (value: BankAccount) => void = () => { throw new Error("No se inició el guardado"); };
    mocks.save.mockImplementation(() => new Promise<BankAccount>(resolve => { finish = resolve; }));
    const wrapper = form();
    try {
      await flushPromises();
      await wrapper.get("form").trigger("submit");
      await wrapper.get("form").trigger("submit");
      expect(mocks.save).toHaveBeenCalledTimes(1);
      finish(account);
      await flushPromises();
      expect(mocks.push).toHaveBeenCalledTimes(1);
    } finally { wrapper.unmount(); }
  });
  it("mantiene el borrador si se rechaza descartar al cancelar", async () => {
    mocks.confirm.mockResolvedValue(false);
    const wrapper = form();
    try {
      await flushPromises();
      await wrapper.get("#holder").setValue("Pendiente");
      const cancel = wrapper.findAll("button").find(button => button.text() === "Cancelar");
      expect(cancel).toBeDefined();
      await cancel?.trigger("click");
      await flushPromises();
      expect(mocks.confirm).toHaveBeenCalledTimes(1);
      expect(mocks.push).not.toHaveBeenCalled();
      expect(wrapper.get<HTMLInputElement>("#holder").element.value).toBe("Pendiente");
    } finally { wrapper.unmount(); }
  });
});
