<script setup lang="ts">
import { ref } from "vue";
import { useRoute } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Message from "primevue/message";
import PageHeader from "../../components/PageHeader.vue";
import FormField from "../../components/FormField.vue";
import EntitySelect from "../../components/EntitySelect.vue";
import PortalPaymentPage from "../portal/PortalPaymentPage.vue";
const route = useRoute();
const clientId = ref<number | null>(null);
const clientName = ref("");
const chosenClient = ref<{ id: number; name: string } | null>(null);
function continueWithClient() {
  if (clientId.value != null) chosenClient.value = { id: clientId.value, name: clientName.value };
}
</script>
<template>
  <template v-if="route.params.id">
    <PageHeader title="Pagos" />
    <Message severity="info" class="mb-4">Los datos originales se conservan. Revisá el pago desde su detalle.</Message>
    <Button as="router-link" :to="'/payments/' + route.params.id" label="Volver al pago" />
  </template>
  <PortalPaymentPage v-else-if="chosenClient" :client-id="chosenClient.id" :client-name="chosenClient.name" />
  <template v-else>
    <PageHeader title="Informar un pago" description="Seleccioná primero el cliente para continuar con la presentación." />
    <Card><template #content>
      <form class="flex flex-col gap-5" @submit.prevent="continueWithClient">
        <FormField id="submissionClient" label="Cliente" required>
          <EntitySelect input-id="submissionClient" v-model="clientId" :label="clientName" kind="clients" @selected="clientName = $event?.label ?? ''" />
        </FormField>
        <div class="flex flex-wrap justify-end gap-3">
          <Button as="router-link" to="/payments" label="Cancelar" severity="secondary" text />
          <Button type="submit" label="Continuar" icon="pi pi-arrow-right" :disabled="clientId == null" />
        </div>
      </form>
    </template></Card>
  </template>
</template>
