<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import InputText from "primevue/inputtext";
import ToggleSwitch from "primevue/toggleswitch";
import Message from "primevue/message";
import Skeleton from "primevue/skeleton";
import PageHeader from "../../components/PageHeader.vue";
import FormField from "../../components/FormField.vue";
import { useDraft } from "../../composables/useDraft";
import { detail, saveRecord } from "../../services/admin";
import type { Bank, BankInput } from "../../services/parameters";
const route = useRoute();
const router = useRouter();
const id = route.params.id ? Number(route.params.id) : undefined;
const form = ref<BankInput>({ name: "", active: true });
const loading = ref(true);
const loadError = ref("");
const { saving, error, fields, save, reset, discard } = useDraft(form);
onMounted(async () => {
  try {
    if (id !== undefined && (!Number.isSafeInteger(id) || id <= 0))
      throw new Error("El identificador del registro es inválido.");
    if (id) {
      const value = await detail<Bank>("banks", id);
      form.value = { name: value.name, active: value.active };

    }
    reset();
  } catch (failure) {
    loadError.value = failure instanceof Error ? failure.message : "No se pudo cargar el registro.";
  } finally { loading.value = false; }
});
async function submit() {
  const result = await save(() => saveRecord<Bank>("banks", form.value, id));
  if (result) await router.push("/parameters/banks");
}
async function cancel() {
  if (await discard()) await router.push("/parameters/banks");
}
</script>
<template>
  <PageHeader :title="id ? 'Editar banco' : 'Nuevo banco'" description="Administrá el nombre y la disponibilidad del banco.">
    <Button as="router-link" to="/parameters/banks" label="Volver al listado" text icon="pi pi-arrow-left" />
  </PageHeader>
  <Skeleton v-if="loading" height="20rem" />
  <Message v-else-if="loadError" severity="error">{{ loadError }}</Message>
  <form v-else @submit.prevent="submit" :aria-busy="saving" class="flex flex-col gap-6">
    <Message v-if="error" severity="error" role="alert">{{ error }}</Message>
    <Card><template #title>Datos del banco</template><template #content>
      <div class="grid gap-5 md:grid-cols-2">

          <FormField id="name" label="Nombre del banco" required :error="fields.name" v-slot="field">
            <InputText id="name" v-model="form.name" required maxlength="255"
              :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
        <FormField id="active" label="Activo" :error="fields.active" v-slot="field" hint="Al desactivarlo, sus cuentas dejan de estar disponibles para nuevos pagos.">
          <ToggleSwitch v-model="form.active" input-id="active" :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" />
        </FormField>
      </div>
    </template></Card>
    <div class="flex flex-wrap justify-end gap-3">
      <Button type="button" label="Cancelar" severity="secondary" outlined :disabled="saving" @click="cancel" />
      <Button type="submit" label="Guardar banco" icon="pi pi-check" :loading="saving" />
    </div>
  </form>
</template>
