<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Button from "primevue/button";
import Message from "primevue/message";
import Skeleton from "primevue/skeleton";
import PageHeader from "../../components/PageHeader.vue";
import ClientEditor from "./ClientEditor.vue";
import { clientsApi, type Client } from "./api";
const route = useRoute();
const router = useRouter();
const id = route.params.id ? Number(route.params.id) : undefined;
const client = ref<Client>();
const loading = ref(!!id);
const error = ref("");
onMounted(async () => {
  if (id) {
    try {
      client.value = await clientsApi.detail(id);
    } catch (failure) {
      error.value = (failure as Error).message;
    } finally {
      loading.value = false;
    }
  }
});
</script>
<template>
  <PageHeader
    :title="id ? 'Editar cliente' : 'Nuevo cliente'"
    description="Información personal y datos de contacto."
    ><Button
      as="router-link"
      to="/clients"
      label="Volver a clientes"
      icon="pi pi-arrow-left"
      text
  /></PageHeader>
  <Skeleton v-if="loading" height="20rem" />
  <Message v-else-if="error" severity="error">{{ error }}</Message>
  <ClientEditor
    v-else
    :client="client"
    @saved="router.push(`/clients/${$event.id}`)"
    @cancel="router.push(id ? `/clients/${id}` : '/clients')"
  />
</template>
