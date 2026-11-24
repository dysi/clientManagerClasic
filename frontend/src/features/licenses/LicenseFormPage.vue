<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import Button from "primevue/button";
import PageHeader from "../../components/PageHeader.vue";
import LicenseEditor from "./LicenseEditor.vue";
import type { License } from "../../services/admin";

const route = useRoute();
const router = useRouter();
const id = route.params.id ? Number(route.params.id) : undefined;
function saved(license: License) {
  return router.push({ path: "/licenses", query: { clientId: license.clientId } });
}
</script>
<template>
  <PageHeader :title="id ? 'Editar licencia' : 'Nueva licencia'"
    description="Configuración de la empresa y condiciones de la licencia.">
    <Button as="router-link" to="/licenses" label="Volver a licencias" icon="pi pi-arrow-left" text />
  </PageHeader>
  <LicenseEditor :id="id" :client-id="route.query.clientId ? Number(route.query.clientId) : undefined"
    :client-name="String(route.query.clientName || '')" @saved="saved" @cancel="router.push('/licenses')" />
</template>
