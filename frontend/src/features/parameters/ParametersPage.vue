<script setup lang="ts">
import Button from "primevue/button";
import Card from "primevue/card";
import PageHeader from "../../components/PageHeader.vue";
</script>
<template>
  <PageHeader title="Parámetros" description="Configurá los bancos y las cuentas destinatarias de transferencias." />
  <div class="grid gap-6 md:grid-cols-2">
    <Card><template #title>Bancos</template><template #content>
      <p class="mb-5 text-muted-color">Administrá los bancos y su disponibilidad.</p>
      <Button as="router-link" to="/parameters/banks" label="Administrar bancos" icon="pi pi-building-columns" />
    </template></Card>
    <Card><template #title>Cuentas bancarias</template><template #content>
      <p class="mb-5 text-muted-color">Administrá titulares, CBU y alias. Desactivá las cuentas que ya no reciban transferencias.</p>
      <Button as="router-link" to="/parameters/bank-accounts" label="Administrar cuentas" icon="pi pi-wallet" />
    </template></Card>
  </div>
</template>
