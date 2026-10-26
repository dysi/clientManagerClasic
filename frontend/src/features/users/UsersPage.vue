<script setup lang="ts">
import { useRoute } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import Tag from "primevue/tag";
import Message from "primevue/message";
import PageHeader from "../../components/PageHeader.vue";
import ListToolbar from "../../components/ListToolbar.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import { usePagedList } from "../../composables/usePagedList";
import { roleOptions, type User } from "../../services/admin";
const route = useRoute();
const clientId = route.query.clientId
  ? Number(route.query.clientId)
  : undefined;
const { rows, keyword, page, size, total, loading, error, search, paginate } =
  usePagedList<User>("/web/api/users", () => ({ clientId }));
function roles(values: string[]) {
  return values
    .map(
      (role) =>
        roleOptions.find((option) => option.value === role)?.label || role,
    )
    .join(", ");
}
</script>
<template>
  <PageHeader
    title="Usuarios"
    description="Cuentas, permisos y vinculación con clientes."
    ><Button
      as="router-link"
      :to="{ path: '/users/new', query: clientId ? { clientId } : {} }"
      label="Nuevo usuario"
      icon="pi pi-plus"
  /></PageHeader>
  <Message v-if="clientId" severity="info" class="mb-4"
    >Usuarios vinculados al cliente #{{ clientId }}.
    <RouterLink to="/users" class="underline">Ver todos</RouterLink></Message
  >
  <Message v-if="error" severity="error" class="mb-4">{{ error }}</Message>
  <Card
    ><template #content>
      <ListToolbar
        v-model="keyword"
        :loading="loading"
        placeholder="Usuario, nombre o correo"
        @search="search"
      />
      <DataTable
        :value="rows"
        :loading="loading"
        data-key="id"
        row-hover
        striped-rows
        scrollable
      >
        <template #empty
          >No se encontraron usuarios con esta búsqueda.</template
        >
        <Column header="Usuario"
          ><template #body="{ data }"
            ><span class="font-medium">{{ data.username }}</span>
            <p class="mt-1 text-sm text-muted-color">
              {{ data.firstname }} {{ data.lastname }}
            </p></template
          ></Column
        >
        <Column header="Acceso"
          ><template #body="{ data }"
            ><Tag
              :value="data.enabled ? 'Habilitado' : 'Deshabilitado'"
              :severity="data.enabled ? 'success' : 'secondary'"
            />
            <p class="mt-2 text-sm text-muted-color">
              {{ roles(data.roles) }}
            </p></template
          ></Column
        >
        <Column header="Cliente"
          ><template #body="{ data }"
            ><RouterLink
              v-if="data.clientId"
              :to="`/clients/${data.clientId}`"
              class="text-primary"
              >{{ data.clientName }}</RouterLink
            ><span v-else class="text-muted-color"
              >Sin cliente vinculado</span
            ></template
          ></Column
        >
        <Column header="Acciones"
          ><template #body="{ data }"
            ><div class="flex flex-wrap gap-1">
              <Button
                as="router-link"
                :to="`/users/${data.id}/edit`"
                label="Editar"
                icon="pi pi-pencil"
                text
                size="small"
              /><Button
                as="router-link"
                :to="`/users/${data.id}/password`"
                label="Cambiar contraseña"
                icon="pi pi-lock"
                text
                size="small"
              /></div></template
        ></Column> </DataTable
      ><ListPaginator
        :page="page"
        :size="size"
        :total="total"
        @change="paginate"
      /> </template
  ></Card>
</template>
