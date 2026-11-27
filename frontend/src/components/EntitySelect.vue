<script setup lang="ts">
import { ref, watch } from "vue";
import AutoComplete from "primevue/autocomplete";
import Message from "primevue/message";
import { request } from "../services/http";
export interface Option {
  id: number;
  label: string;
}
const props = defineProps<{
  modelValue: number | null;
  label?: string;
  kind: "clients" | "licenses" | "banks" | "portal-licenses" | "payment-bank-accounts";
  clientId?: number;
  inputId: string;
  disabled?: boolean;
  invalid?: boolean;
  describedby?: string;
}>();
const emit = defineEmits<{ "update:modelValue": [id: number | null]; selected: [option: Option | null] }>();
const selected = ref<Option | string | null>(null);
const suggestions = ref<Option[]>([]);
const error = ref("");
let sequence = 0;
watch(
  () => [props.modelValue, props.label] as const,
  ([id, label]) => {
    if (id != null) {
      const current = selected.value;
      if (
        current &&
        typeof current === "object" &&
        current.id === id &&
        !current.label.startsWith("#")
      )
        return;
      selected.value = { id, label: label || `#${id}` };
    } else if (typeof selected.value !== "string") selected.value = null;
  },
  { immediate: true },
);
async function search(event: { query: string }) {
  const current = ++sequence;
  error.value = "";
  try {
    const params = new URLSearchParams({ keyword: event.query, size: "25" });
    const result = props.kind === "portal-licenses"
      ? (await request<{ content: { id: number; companyName: string }[] }>(props.clientId == null ? `/web/portal/licenses?${params}` : `/web/api/clients/${props.clientId}/payment-submissions/licenses?${params}`))
          .content.map((license) => ({ id: license.id, label: license.companyName }))
      : await request<Option[]>(`/web/api/options/${props.kind}?${params}`);
    if (current === sequence) suggestions.value = result;
  } catch {
    if (current === sequence)
      error.value = "No se pudieron cargar las opciones. Volvé a buscar.";
  }
}
function update(value: Option | string | null) {
  selected.value = value;
    emit("selected", typeof value === "object" && value ? value : null);
  emit(
    "update:modelValue",
    typeof value === "object" && value ? value.id : null,
  );
}
</script>
<template>
  <AutoComplete
    :input-id="inputId"
    :model-value="selected"
    :suggestions="suggestions"
    option-label="label"
    dropdown
    :pt="{
      dropdown: {
        'aria-label':
          { clients: 'Mostrar clientes', licenses: 'Mostrar licencias', banks: 'Mostrar bancos', 'payment-bank-accounts': 'Mostrar cuentas bancarias', 'portal-licenses': 'Mostrar mis licencias' }[kind],
      },
    }"
    force-selection
    :disabled="disabled"
    :invalid="invalid"
    :input-props="{ 'aria-describedby': describedby }"
    placeholder="Escribí para buscar o abrí las opciones"
    class="w-full"
    fluid
    @complete="search"
    @update:model-value="update"
  />
  <Message v-if="error" severity="error" size="small" variant="simple">{{
    error
  }}</Message>
</template>
