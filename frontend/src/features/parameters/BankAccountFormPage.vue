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
import EntitySelect from "../../components/EntitySelect.vue";
import { useDraft } from "../../composables/useDraft";
import { detail, saveRecord } from "../../services/admin";
import type { BankAccount, BankAccountInput } from "../../services/parameters";
const route = useRoute();
const router = useRouter();
const id = route.params.id ? Number(route.params.id) : undefined;
const form = ref<BankAccountInput>({ bankId: null, holder: "", cbu: "", alias: "", externalCode: "", active: true });
const bankName = ref("");
const loading = ref(true);
const loadError = ref("");
const { saving, error, fields, save, reset, discard } = useDraft(form);
onMounted(async () => {
  try {
    if (id !== undefined && (!Number.isSafeInteger(id) || id <= 0))
      throw new Error("El identificador del registro es inválido.");
    if (id) {
      const value = await detail<BankAccount>("bank-accounts", id);
      form.value = { bankId: value.bankId, holder: value.holder, cbu: value.cbu, alias: value.alias || "", externalCode: value.externalCode ?? "", active: value.active };
      bankName.value = value.bankName;
    }
    reset();
  } catch (failure) {
    loadError.value = failure instanceof Error ? failure.message : "No se pudo cargar el registro.";
  } finally { loading.value = false; }
});
async function submit() {
  const result = await save(() => saveRecord<BankAccount>("bank-accounts", form.value, id));
  if (result) await router.push("/parameters/bank-accounts");
}
async function cancel() {
  if (await discard()) await router.push("/parameters/bank-accounts");
}
</script>
<template>
  <PageHeader :title="id ? 'Editar cuenta bancaria' : 'Nueva cuenta bancaria'" description="Los cambios se aplican a futuras transferencias; los pagos conservan sus datos originales.">
    <Button as="router-link" to="/parameters/bank-accounts" label="Volver al listado" text icon="pi pi-arrow-left" />
  </PageHeader>
  <Skeleton v-if="loading" height="20rem" />
  <Message v-else-if="loadError" severity="error">{{ loadError }}</Message>
  <form v-else @submit.prevent="submit" :aria-busy="saving" class="flex flex-col gap-6">
    <Message v-if="error" severity="error" role="alert">{{ error }}</Message>
    <Card><template #title>Datos de la cuenta</template><template #content>
      <div class="grid gap-5 md:grid-cols-2">
        <FormField id="bankId" label="Banco" required :error="fields.bankId" v-slot="field">
          <EntitySelect v-model="form.bankId" :label="bankName" kind="banks" input-id="bankId"
            :disabled="saving" :invalid="field.invalid" :describedby="field.describedby" />
        </FormField>
          <FormField id="holder" label="Titular" required :error="fields.holder" v-slot="field">
            <InputText id="holder" v-model="form.holder" required maxlength="255"
              :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
          <FormField id="cbu" label="CBU" required :error="fields.cbu" v-slot="field" hint="22 dígitos; podés pegar espacios o guiones.">
            <InputText id="cbu" v-model="form.cbu" required maxlength="64" inputmode="numeric"
              :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
          <FormField id="alias" label="Alias"  :error="fields.alias" v-slot="field">
            <InputText id="alias" v-model="form.alias"  maxlength="100"
              :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
        <FormField id="externalCode" label="Código externo" :error="fields.externalCode" v-slot="field" hint="Opcional. Hasta 100 caracteres.">
          <InputText id="externalCode" v-model="form.externalCode" maxlength="100"
            :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
        </FormField>
        <FormField id="active" label="Activa" :error="fields.active" v-slot="field" hint="Una cuenta activa solo estará disponible si su banco también está activo.">
          <ToggleSwitch v-model="form.active" input-id="active" :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" />
        </FormField>
      </div>
    </template></Card>
    <div class="flex flex-wrap justify-end gap-3">
      <Button type="button" label="Cancelar" severity="secondary" outlined :disabled="saving" @click="cancel" />
      <Button type="submit" label="Guardar cuenta" icon="pi pi-check" :loading="saving" />
    </div>
  </form>
</template>
