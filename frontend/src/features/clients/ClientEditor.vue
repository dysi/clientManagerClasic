<script setup lang="ts">
import { ref } from "vue";
import Button from "primevue/button";
import Card from "primevue/card";
import InputText from "primevue/inputtext";
import Message from "primevue/message";
import FormField from "../../components/FormField.vue";
import { useDraft } from "../../composables/useDraft";
import { clientDraft, clientsApi, emptyClient, type Client } from "./api";
const props = defineProps<{ client?: Client }>();
const emit = defineEmits<{ saved: [client: Client]; cancel: [] }>();
const form = ref(props.client ? clientDraft(props.client) : emptyClient());
const { dirty, saving, error, fields, save, discard } = useDraft(form);
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
    <Card
      ><template #title>Datos del cliente</template
      ><template #subtitle>Los campos con * son obligatorios.</template
      ><template #content>
        <Message v-if="error" severity="error" class="mb-5" role="alert">{{
          error
        }}</Message>
        <div class="grid gap-5 md:grid-cols-2">
          <FormField id="legalName" label="Razón social" :error="fields.legalName" v-slot="field">
            <InputText id="legalName" v-model="form.legalName" maxlength="255" :disabled="saving"
              :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
          <FormField id="tradeName" label="Nombre de fantasía" :error="fields.tradeName" v-slot="field"
            hint="Nombre por el que se conoce al comercio, para facilitar las búsquedas.">
            <InputText id="tradeName" v-model="form.tradeName" maxlength="255" :disabled="saving"
              :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
          <FormField id="externalCode" label="Código externo" :error="fields.externalCode" v-slot="field"
            hint="Identificador opcional para vincular este cliente con sistemas remotos.">
            <InputText id="externalCode" v-model="form.externalCode" maxlength="100" :disabled="saving"
              :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
          <FormField id="dni" label="DNI" :error="fields.dni" v-slot="field"
            hint="Hasta ocho dígitos, sin puntos ni guiones. Conservá los ceros iniciales si están registrados.">
            <InputText id="dni" v-model="form.dni" inputmode="numeric" maxlength="8" pattern="[0-9]{0,8}"
              :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
          <FormField
            id="firstname"
            label="Nombre"
            required
            :error="fields.firstname"
            v-slot="field"
            ><InputText
              id="firstname"
              v-model="form.firstname"
              required
              maxlength="100"
              :disabled="saving"
              :invalid="field.invalid"
              :aria-describedby="field.describedby"
              fluid
          /></FormField>
          <FormField
            id="lastname"
            label="Apellido"
            required
            :error="fields.lastname"
            v-slot="field"
            ><InputText
              id="lastname"
              v-model="form.lastname"
              required
              maxlength="100"
              :disabled="saving"
              :invalid="field.invalid"
              :aria-describedby="field.describedby"
              fluid
          /></FormField>
          <FormField
            id="email"
            label="Correo electrónico"
            :error="fields.email"
            v-slot="field"
            ><InputText
              id="email"
              v-model="form.email"
              type="email"
              maxlength="255"
              :disabled="saving"
              :invalid="field.invalid"
              :aria-describedby="field.describedby"
              fluid
          /></FormField>
          <FormField
            id="phone"
            label="Teléfono"
            :error="fields.phone"
            v-slot="field"
            ><InputText
              id="phone"
              v-model="form.phone"
              type="tel"
              maxlength="50"
              :disabled="saving"
              :invalid="field.invalid"
              :aria-describedby="field.describedby"
              fluid
          /></FormField>
          <FormField
            id="cuil"
            label="CUIL / CUIT"
            :error="fields.cuil"
            v-slot="field"
            ><InputText
              id="cuil"
              v-model="form.cuil"
              maxlength="32"
              :disabled="saving"
              :invalid="field.invalid"
              :aria-describedby="field.describedby"
              fluid
          /></FormField>
        </div> </template
    ></Card>
    <div class="mt-6 flex flex-wrap justify-end gap-3">
      <Button
        type="button"
        label="Cancelar"
        severity="secondary"
        outlined
        :disabled="saving"
        @click="cancel"
      /><Button
        type="submit"
        label="Guardar cliente"
        icon="pi pi-check"
        :loading="saving"
      />
    </div>
  </form>
</template>
