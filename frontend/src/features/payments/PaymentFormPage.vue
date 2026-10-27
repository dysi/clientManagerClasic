<script setup lang="ts">
import Button from "primevue/button";
import Message from "primevue/message";
import PageHeader from "../../components/PageHeader.vue";
</script>
<template>
  <PageHeader title="Pagos" description="Gestión de pagos informados." />
  <Message severity="info" class="mb-4">Los pagos se presentan desde el portal del cliente y se revisan desde su detalle. Para corregir los datos de una presentación, registrá un nuevo pago; los datos financieros originales se conservan.</Message>
  <Button as="router-link" to="/payments" label="Volver a pagos" icon="pi pi-arrow-left" />
</template>
