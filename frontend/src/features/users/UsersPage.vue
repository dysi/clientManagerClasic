<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import Tag from "primevue/tag";
import Message from "primevue/message";
import Select from "primevue/select";
import EntitySelect from "../../components/EntitySelect.vue";
import FormField from "../../components/FormField.vue";
import PageHeader from "../../components/PageHeader.vue";
import ListToolbar from "../../components/ListToolbar.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import { usePagedList } from "../../composables/usePagedList";
import { roleOptions, type User } from "../../services/admin";
const route = useRoute();
function routeClientId(): number | null {
  const id = Number(route.query.clientId);
  return Number.isSafeInteger(id) && id > 0 ? id : null;
}
const clientId = ref<number | null>(routeClientId());
const enabled = ref<string | null>(null);
const authority = ref<string | null>(null);
const states = [
  { label: "Habilitado", value: "true" },
  { label: "Deshabilitado", value: "false" },
];
const { rows, keyword, page, size, total, loading, error, search, paginate } =
  usePagedList<User>("/web/api/users", () => ({
    clientId: clientId.value,
    enabled: enabled.value,
    authority: authority.value,
  }));
watch(() => route.query.clientId, () => {
  clientId.value = routeClientId();
});
watch([clientId, enabled, authority], () => {
  void search();
});
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
  <Message v-if="error" severity="error" class="mb-4">{{ error }}</Message>
  <Card
    ><template #content>
      <ListToolbar
        v-model="keyword"
        :loading="loading"
        placeholder="Usuario, nombre o correo"
        @search="search"
      />
      <div class="mb-4 grid min-w-0 grid-cols-1 gap-4 md:grid-cols-3">
        <FormField id="users-enabled" label="Estado">
          <Select
            v-model="enabled"
            input-id="users-enabled"
            :options="states"
            option-label="label"
            option-value="value"
            placeholder="Todos los estados"
            show-clear
            fluid
          />
        </FormField>
        <FormField id="users-client" label="Cliente">
          <EntitySelect v-model="clientId" input-id="users-client" kind="clients" />
          <Button v-if="clientId" type="button" label="Limpiar cliente" text size="small" @click="clientId = null" />
        </FormField>
        <FormField id="users-authority" label="Permiso asignado">
          <Select
            v-model="authority"
            input-id="users-authority"
            :options="roleOptions"
            option-label="label"
            option-value="value"
            placeholder="Todos los permisos"
            show-clear
            fluid
          />
        </FormField>
      </div>
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
