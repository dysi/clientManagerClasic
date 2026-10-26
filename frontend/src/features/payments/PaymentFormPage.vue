<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import InputNumber from "primevue/inputnumber";
import Textarea from "primevue/textarea";
import Message from "primevue/message";
import Skeleton from "primevue/skeleton";
import PageHeader from "../../components/PageHeader.vue";
import FormField from "../../components/FormField.vue";
import EntitySelect from "../../components/EntitySelect.vue";
import DateInput from "../../components/DateInput.vue";
import { useDraft } from "../../composables/useDraft";
import {
  detail,
  saveRecord,
  type Payment,
  type License,
} from "../../services/admin";
import { today, money } from "../../services/format";
const route = useRoute();
const router = useRouter();
const id = route.params.id ? Number(route.params.id) : undefined;
const form = ref({
  licenseId: route.query.licenseId
    ? Number(route.query.licenseId)
    : (null as number | null),
  amount: 0 as number | null,
  discount: 0 as number | null,
  paymentDate: today() as string | null,
  expirationDate: today(30) as string | null,
  paymentRealDate: null as string | null,
  observation: "",
});
const companyName = ref("");
const loading = ref(true);
const loadError = ref("");
const { error, fields, saving, reset, save } = useDraft(form);
onMounted(async () => {
  try {
    if (id) {
      const value = await detail<Payment>("payments", id);
      if (value.removed)
        throw new Error("Reactivá el pago desde el listado antes de editarlo.");
      form.value = {
        licenseId: value.licenseId,
        amount: value.amount,
        discount: value.discount,
        paymentDate: value.paymentDate,
        expirationDate: value.expirationDate,
        paymentRealDate: value.paymentRealDate,
        observation: value.observation || "",
      };
      companyName.value = value.companyName;
    } else if (form.value.licenseId)
      companyName.value = (
        await detail<License>("licenses", form.value.licenseId)
      ).companyName;
    reset();
  } catch (failure) {
    loadError.value = (failure as Error).message;
  } finally {
    loading.value = false;
  }
});
async function submit() {
  const result = await save(() =>
    saveRecord<Payment>("payments", form.value, id),
  );
  if (result)
    await router.push({
      path: "/payments",
      query: { licenseId: result.licenseId, companyName: result.companyName },
    });
}
</script>
<template>
  <PageHeader
    :title="id ? 'Editar pago' : 'Nuevo pago'"
    description="Registro administrativo del período y del cobro; no procesa pagos electrónicos."
    ><Button
      as="router-link"
      to="/payments"
      label="Volver a pagos"
      text
      icon="pi pi-arrow-left"
  /></PageHeader>
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
      ><template #title>Licencia e importes</template
      ><template #content
        ><div class="grid gap-5 md:grid-cols-3">
          <FormField
            id="licenseId"
            label="Licencia"
            required
            :error="fields.licenseId"
            v-slot="field"
            ><EntitySelect
              v-model="form.licenseId"
              :label="companyName"
              kind="licenses"
              input-id="licenseId"
              :disabled="saving || !!id"
              :invalid="field.invalid"
              :describedby="field.describedby"
          /></FormField>
          <FormField
            id="amount"
            label="Importe"
            required
            :error="fields.amount"
            v-slot="field"
            ><InputNumber
              input-id="amount"
              v-model="form.amount"
              mode="currency"
              currency="ARS"
              locale="es-AR"
              :min="0"
              :max="999999999999.99"
              :disabled="saving"
              :invalid="field.invalid"
              fluid
          /></FormField>
          <FormField
            id="discount"
            label="Descuento"
            required
            :error="fields.discount"
            v-slot="field"
            ><InputNumber
              input-id="discount"
              v-model="form.discount"
              mode="currency"
              currency="ARS"
              locale="es-AR"
              :min="0"
              :max="form.amount || 0"
              :disabled="saving"
              :invalid="field.invalid"
              fluid
          /></FormField>
        </div>
        <p class="mt-5 font-medium">
          Importe neto: {{ money((form.amount || 0) - (form.discount || 0)) }}
        </p></template
      ></Card
    >
    <Card
      ><template #title>Período y cobro</template
      ><template #content
        ><div class="grid gap-5 md:grid-cols-3">
          <FormField
            id="paymentDate"
            label="Fecha del período"
            required
            :error="fields.paymentDate"
            v-slot="field"
            ><DateInput
              v-model="form.paymentDate"
              input-id="paymentDate"
              required
              :disabled="saving"
              :invalid="field.invalid"
              :describedby="field.describedby"
          /></FormField>
          <FormField
            id="expirationDate"
            label="Fecha de vencimiento"
            :error="fields.expirationDate"
            v-slot="field"
            ><DateInput
              v-model="form.expirationDate"
              input-id="expirationDate"
              :disabled="saving"
              :invalid="field.invalid"
              :describedby="field.describedby"
          /></FormField>
          <FormField
            id="paymentRealDate"
            label="Fecha de cobro"
            hint="Dejar vacía mientras el pago esté pendiente."
            :error="fields.paymentRealDate"
            v-slot="field"
            ><DateInput
              v-model="form.paymentRealDate"
              input-id="paymentRealDate"
              :disabled="saving"
              :invalid="field.invalid"
              :describedby="field.describedby"
          /></FormField>
        </div>
        <div class="mt-5">
          <FormField
            id="observation"
            label="Observaciones"
            :error="fields.observation"
            v-slot="field"
            ><Textarea
              id="observation"
              v-model="form.observation"
              rows="3"
              auto-resize
              maxlength="255"
              :disabled="saving"
              :invalid="field.invalid"
              fluid
          /></FormField></div></template
    ></Card>
    <Message severity="info"
      >Registrar un pago no modifica automáticamente la fecha de bloqueo de la
      licencia.</Message
    >
    <div class="flex flex-wrap justify-end gap-3">
      <Button
        as="router-link"
        to="/payments"
        label="Cancelar"
        severity="secondary"
        outlined
        :disabled="saving"
      /><Button
        type="submit"
        label="Guardar pago"
        icon="pi pi-check"
        :loading="saving"
      />
    </div>
  </form>
</template>
