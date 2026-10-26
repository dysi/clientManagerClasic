import { mount } from "@vue/test-utils";
import { describe, it, expect } from "vitest";
import PrimeVue from "primevue/config";
import AutoComplete from "primevue/autocomplete";
import EntitySelect from "./EntitySelect.vue";

describe("Selector de relaciones", () => {
  it("conserva el nombre elegido cuando el formulario actualiza el identificador", async () => {
    const wrapper = mount(EntitySelect, {
      props: { modelValue: null, kind: "clients", inputId: "client" },
      global: { plugins: [[PrimeVue, { unstyled: true }]] },
    });
    wrapper
      .findComponent(AutoComplete)
      .vm.$emit("update:modelValue", { id: 7, label: "Ana Pérez" });
    await wrapper.setProps({ modelValue: 7 });
    expect(wrapper.findComponent(AutoComplete).props("modelValue")).toEqual({
      id: 7,
      label: "Ana Pérez",
    });
    expect(wrapper.emitted("update:modelValue")?.[0]).toEqual([7]);
    await wrapper.setProps({ modelValue: null });
    expect(wrapper.findComponent(AutoComplete).props("modelValue")).toBeNull();
    wrapper.unmount();
  });
});
