<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Message from "primevue/message";
import Skeleton from "primevue/skeleton";
import PageHeader from "../../components/PageHeader.vue";
import { clientsApi, type Client } from "./api";
const route = useRoute();
const client = ref<Client>();
const error = ref("");
onMounted(async () => {
  try {
    client.value = await clientsApi.detail(Number(route.params.id));
  } catch (failure) {
    error.value = (failure as Error).message;
  }
});
</script>
<template>
  <Message v-if="error" severity="error">{{ error }}</Message>
  <template v-else-if="client">
    <PageHeader
      :title="`${client.firstname} ${client.lastname}`"
      :description="`Cliente #${client.id}`"
      ><Button
        as="router-link"
        to="/clients"
        label="Volver a clientes"
        text /><Button
        as="router-link"
        :to="`/clients/${client.id}/edit`"
        label="Editar datos"
        icon="pi pi-pencil"
    /></PageHeader>
    <div class="grid gap-6 lg:grid-cols-3">
      <Card class="lg:col-span-2"
        ><template #title>Contacto e identificación</template
        ><template #content
          ><dl class="grid gap-6 py-2 sm:grid-cols-2">
            <div>
              <dt class="text-sm text-muted-color">Correo electrónico</dt>
              <dd class="mt-2 break-all">
                {{ client.email || "Sin correo registrado" }}
              </dd>
            </div>
            <div>
              <dt class="text-sm text-muted-color">Teléfono</dt>
              <dd class="mt-2">
                {{ client.phone || "Sin teléfono registrado" }}
              </dd>
            </div>
            <div>
              <dt class="text-sm text-muted-color">CUIL / CUIT</dt>
              <dd class="mt-2">
                {{ client.cuil || "Sin identificación registrada" }}
              </dd>
            </div>
            <div>
              <dt class="text-sm text-muted-color">Usuario vinculado</dt>
              <dd class="mt-2">{{ client.username }}</dd>
            </div>
          </dl></template
        ></Card
      >
      <Card
        ><template #title>Gestión del cliente</template
        ><template #content
          ><div class="flex flex-col gap-4">
            <p class="text-muted-color">
              Cada operación tiene su espacio, con el cliente ya seleccionado.
            </p>
            <Button
              as="router-link"
              :to="{ path: '/licenses', query: { clientId: client.id } }"
              label="Ver licencias"
              icon="pi pi-key"
              outlined
            /><Button
              as="router-link"
              :to="{
                path: '/licenses/new',
                query: {
                  clientId: client.id,
                  clientName: `${client.firstname} ${client.lastname}`,
                },
              }"
              label="Nueva licencia"
              icon="pi pi-plus"
            /><Button
              as="router-link"
              :to="{ path: '/users', query: { clientId: client.id } }"
              label="Ver usuarios vinculados"
              icon="pi pi-users"
              text
            /></div></template
      ></Card>
    </div> </template
  ><Skeleton v-else height="20rem" />
</template>
