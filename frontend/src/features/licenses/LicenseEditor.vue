<script setup lang="ts">
import { onMounted, ref } from "vue";
import Button from "primevue/button";
import Card from "primevue/card";
import InputText from "primevue/inputtext";
import InputNumber from "primevue/inputnumber";
import Textarea from "primevue/textarea";
import ToggleSwitch from "primevue/toggleswitch";
import Message from "primevue/message";
import Skeleton from "primevue/skeleton";
import FormField from "../../components/FormField.vue";
import EntitySelect from "../../components/EntitySelect.vue";
import DateInput from "../../components/DateInput.vue";
import { useDraft } from "../../composables/useDraft";
import { detail, saveRecord, type License, type LicenseInput } from "../../services/admin";
import { today } from "../../services/format";
const props = defineProps<{ id?: number; clientId?: number; clientName?: string }>();
const emit = defineEmits<{ saved: [license: License]; cancel: [] }>();
const id = props.id;
type LicenseDraft = Omit<LicenseInput, "clientId" | "companyId" | "initialActivity"> & {
  clientId: number | null;
  companyId: number | null;
  initialActivity: string | null;
};
const form = ref<LicenseDraft>({
  clientId: props.clientId ?? null,
  companyId: null,
  companyName: "",
  description: "",
  initialActivity: today(),
  lockDate: today(90),
  prePayment: true,
});
const clientName = ref(props.clientName || "");
const loading = ref(true);
const loadError = ref("");
const { error, fields, saving, save, reset, discard } = useDraft(form);
onMounted(async () => {
  try {
    if (id) {
      const value = await detail<License>("licenses", id);
      form.value = {
        clientId: value.clientId,
        companyId: value.companyId,
        companyName: value.companyName,
        description: value.description || "",
        initialActivity: value.initialActivity,
        lockDate: value.lockDate,
        prePayment: value.prePayment,
      };
      clientName.value = value.clientName;
    } else if (form.value.clientId && !clientName.value) {
      const client = await detail<{ firstname: string; lastname: string }>(
        "clients",
        form.value.clientId,
      );
      clientName.value = `${client.firstname} ${client.lastname}`;
    }
    reset();
  } catch (failure) {
    loadError.value = (failure as Error).message;
  } finally {
    loading.value = false;
  }
});
async function submit() {
  const result = await save(() =>
    saveRecord<License>("licenses", form.value, id),
  );
  if (result) emit("saved", result);
}
let closing = false;
async function cancel() {
  if (saving.value || closing) return;
  closing = true;
  try {
    if (await discard()) emit("cancel");
  } finally { closing = false; }
}
defineExpose({ saving, cancel });
</script>
<template>
  <Skeleton v-if="loading" height="24rem" /><Message
    v-else-if="loadError"
    severity="error"
    >{{ loadError }}</Message
  >
  <form
    v-else
    @submit.prevent="submit"
    :aria-busy="saving"
    class="flex flex-col gap-6"
  >
    <Message v-if="error" severity="error">{{ error }}</Message>
    <Card
      ><template #title>Cliente y empresa</template
      ><template #content
        ><div class="grid gap-5 md:grid-cols-2">
          <FormField
            id="clientId"
            label="Cliente"
            required
            :error="fields.clientId"
            :hint="
              id
                ? 'El cliente de una licencia existente no se cambia desde este formulario.'
                : undefined
            "
            v-slot="field"
            ><EntitySelect
              v-model="form.clientId"
              :label="clientName"
              kind="clients"
              input-id="clientId"
              :disabled="saving || !!id"
              :invalid="field.invalid"
              :describedby="field.describedby"
          /></FormField>
          <FormField
            id="companyId"
            label="Identificador de empresa"
            required
            :error="fields.companyId"
            v-slot="field"
            ><InputNumber
              input-id="companyId"
              v-model="form.companyId"
              :use-grouping="false"
              :min="1"
              :max="9007199254740991"
              :disabled="saving"
              :invalid="field.invalid"
              fluid
          /></FormField>
          <FormField
            id="companyName"
            label="Nombre de empresa"
            required
            :error="fields.companyName"
            v-slot="field"
            ><InputText
              id="companyName"
              v-model="form.companyName"
              required
              maxlength="255"
              :disabled="saving"
              :invalid="field.invalid"
              fluid
          /></FormField>
          <FormField
            id="description"
            label="Descripción"
            :error="fields.description"
            v-slot="field"
            ><Textarea
              id="description"
              v-model="form.description"
              maxlength="255"
              rows="2"
              auto-resize
              :disabled="saving"
              :invalid="field.invalid"
              fluid
          /></FormField></div></template
    ></Card>
    <Card
      ><template #title>Vigencia y modalidad</template
      ><template #content
        ><div class="grid gap-5 md:grid-cols-3">
          <FormField
            id="initialActivity"
            label="Inicio de actividades"
            required
            :error="fields.initialActivity"
            v-slot="field"
            ><DateInput
              v-model="form.initialActivity"
              input-id="initialActivity"
              required
              :disabled="saving"
              :invalid="field.invalid"
              :describedby="field.describedby"
          /></FormField>
          <FormField
            id="lockDate"
            label="Fecha de bloqueo"
            required
            :error="fields.lockDate"
            hint="La licencia vence al comenzar esta fecha."
            v-slot="field"
            ><DateInput
              v-model="form.lockDate"
              input-id="lockDate"
              required
              :disabled="saving"
              :invalid="field.invalid"
              :describedby="field.describedby"
          /></FormField>
          <FormField
            id="prePayment"
            label="Pago por adelantado"
            :error="fields.prePayment"
            hint="Desactivado corresponde a mes vencido."
            ><ToggleSwitch
              input-id="prePayment"
              v-model="form.prePayment"
              :disabled="saving"
          /></FormField></div></template
    ></Card>
    <div class="flex flex-wrap justify-end gap-3">
      <Button
        type="button"
        label="Cancelar"
        severity="secondary"
        outlined
        :disabled="saving"
        @click="cancel"
      /><Button
        type="submit"
        label="Guardar licencia"
        icon="pi pi-check"
        :loading="saving"
      />
    </div>
  </form>
</template>
