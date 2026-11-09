<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import Button from "primevue/button";
import Card from "primevue/card";
import Select from "primevue/select";
import Textarea from "primevue/textarea";
import InputText from "primevue/inputtext";
import Message from "primevue/message";
import FormField from "../../components/FormField.vue";
import { useDraft } from "../../composables/useDraft";
import { clientDraft, clientsApi, emptyClient, type Client, type ClientInput, type Place } from "./api";
const props = defineProps<{ client?: Client }>();
const emit = defineEmits<{ saved: [client: Client]; cancel: [] }>();
const form = ref(props.client ? clientDraft(props.client) : emptyClient());
const countries = ref<Place[]>([]);
const provinces = ref<Place[]>([]);
const placesLoading = ref(false);
const placesError = ref("");
let defaultsInitialized = false;
const availableProvinces = computed(() => provinces.value.filter(p => p.countryId === form.value.countryId));
async function loadPlaces() {
  placesLoading.value = true;
  placesError.value = "";
  try {
    const [countryRows, provinceRows] = await Promise.all([clientsApi.countries(), clientsApi.provinces()]);
    countries.value = countryRows;
    provinces.value = provinceRows;
    if (!props.client && !defaultsInitialized) {
      const wasDirty = dirty.value;
      const argentina = countryRows.find(country => country.name === "Argentina");
      const mendoza = provinceRows.find(province =>
        province.name === "Mendoza" && province.countryId === argentina?.id);
      form.value.countryId = argentina?.id ?? 0;
      form.value.provinceId = mendoza?.id ?? 0;
      defaultsInitialized = true;
      // Los valores iniciales no son una edición del usuario.
      if (!wasDirty) reset();
    }
  } catch (failure) {
    placesError.value = failure instanceof Error ? failure.message : "No se pudieron cargar países y provincias.";
  } finally { placesLoading.value = false; }
}
onMounted(loadPlaces);
interface TextField { id: Exclude<keyof ClientInput, "countryId" | "provinceId">; label: string; max: number; required?: boolean; type?: string }
const mainFields: TextField[] = [
  { id: "legalName", label: "Razón social", max: 255 },
  { id: "tradeName", label: "Nombre de fantasía", max: 255 },
  { id: "firstname", label: "Nombre", max: 100, required: true },
  { id: "lastname", label: "Apellido", max: 100, required: true },
  { id: "dni", label: "DNI", max: 8 },
  { id: "cuil", label: "CUIL / CUIT", max: 32 },
  { id: "externalCode", label: "Código externo", max: 100 },
];
const contactFields: TextField[] = [
  { id: "email", label: "Correo electrónico", max: 255, type: "email" },
  { id: "phone", label: "Teléfono", max: 50, type: "tel" },
  { id: "street", label: "Calle", max: 255 },
  { id: "streetNumber", label: "Número", max: 30 },
  { id: "addressComplement", label: "Número interior / Piso / Departamento / Oficina", max: 255 },
  { id: "neighborhood", label: "Barrio / Sector", max: 255 },
  { id: "city", label: "Ciudad / Localidad", max: 255 },
  { id: "postalCode", label: "Código Postal (CP)", max: 30 },
];
const { dirty, saving, error, fields, reset, save, discard } = useDraft(form);
async function submit() {
  const result = await save(() =>
    clientsApi.save(form.value, props.client?.id),
  );
  if (result) emit("saved", result);
}
async function cancel() {
  if (await discard()) emit("cancel");
}
defineExpose({ changed: dirty, saving });
</script>
<template>
  <form @submit.prevent="submit" :aria-busy="saving">
    <Message v-if="error" severity="error" class="mb-5" role="alert">{{ error }}</Message>
    <div class="grid min-w-0 gap-6 xl:grid-cols-2">
      <Card><template #title>Datos principales</template>
        <template #subtitle>Los campos con * son obligatorios.</template>
        <template #content><div class="grid gap-5 sm:grid-cols-2">
          <FormField v-for="item in mainFields" :key="item.id" :id="item.id" :label="item.label"
            :required="item.required" :error="fields[item.id]" v-slot="field">
            <InputText :id="item.id" v-model="form[item.id]" :maxlength="item.max" :required="item.required"
              :inputmode="item.id === 'dni' ? 'numeric' : undefined" :pattern="item.id === 'dni' ? '[0-9]{0,8}' : undefined"
              :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
        </div></template>
      </Card>
      <Card><template #title>Contacto y dirección</template>
        <template #content><div class="grid gap-5 sm:grid-cols-2">
          <FormField v-for="item in contactFields" :key="item.id" :id="item.id" :label="item.label"
            :error="fields[item.id]" v-slot="field">
            <InputText :id="item.id" v-model="form[item.id]" :type="item.type || 'text'" :maxlength="item.max"
              :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
          <div v-if="placesError" class="sm:col-span-2">
            <Message severity="error">{{ placesError }}</Message>
            <Button type="button" label="Reintentar cargar países y provincias" text @click="loadPlaces" />
          </div>
          <FormField id="countryId" label="País" :error="fields.countryId" v-slot="field">
            <Select input-id="countryId" v-model="form.countryId" :options="[{ id: 0, name: 'Sin registrar' }, ...countries]"
              option-label="name" option-value="id" :loading="placesLoading" :disabled="saving || placesLoading || !!placesError"
              :invalid="field.invalid" :aria-describedby="field.describedby" @change="form.provinceId = 0" fluid />
          </FormField>
          <FormField id="provinceId" label="Provincia" :error="fields.provinceId" v-slot="field">
            <Select input-id="provinceId" v-model="form.provinceId" :options="[{ id: 0, name: 'Sin registrar' }, ...availableProvinces]"
              option-label="name" option-value="id" :disabled="saving || placesLoading || !!placesError || !form.countryId"
              :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
        </div></template>
      </Card>
      <Card class="xl:col-span-2"><template #title>Observaciones</template>
        <template #content>
          <FormField id="observations" label="Observaciones generales del cliente" :error="fields.observations"
            hint="Podés agregar indicaciones de entrega, horarios de recepción u otras notas del cliente." v-slot="field">
            <Textarea id="observations" v-model="form.observations" rows="4" maxlength="4000" auto-resize
              :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
        </template>
      </Card>
    </div>
    <div class="mt-6 flex flex-wrap justify-end gap-3">
      <Button type="button" label="Cancelar" severity="secondary" outlined :disabled="saving" @click="cancel" />
      <Button type="submit" label="Guardar cliente" icon="pi pi-check" :loading="saving" />
    </div>
  </form>
</template>
