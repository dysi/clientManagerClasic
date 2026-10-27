<script setup lang="ts">
import Button from "primevue/button";
import Card from "primevue/card";
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import Message from "primevue/message";
import PageHeader from "../../components/PageHeader.vue";
import ListToolbar from "../../components/ListToolbar.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import { usePagedList } from "../../composables/usePagedList";
import type { Client } from "./api";
const { rows, keyword, loading, error, page, size, total, search, paginate } =
  usePagedList<Client>("/web/api/clients");
</script>
<template>
  <PageHeader
    title="Clientes"
    description="Directorio de personas y empresas vinculadas a tus licencias."
    ><Button
      as="router-link"
      to="/clients/new"
      label="Nuevo cliente"
      icon="pi pi-plus"
  /></PageHeader>
  <Message v-if="error" severity="error" class="mb-4">{{ error }}</Message>
  <Card
    ><template #content>
      <ListToolbar
        v-model="keyword"
        :loading="loading"
        placeholder="Nombre, razón social, fantasía, correo, CUIL o usuario"
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
          >No se encontraron clientes. Probá otra búsqueda o creá un
          cliente.</template
        >
        <Column header="Cliente"
          ><template #body="{ data }"
            ><RouterLink
              :to="`/clients/${data.id}`"
              class="font-medium text-primary"
              >{{ data.lastname }}, {{ data.firstname }}</RouterLink
            >
            <p v-if="data.tradeName" class="mt-1 break-words text-sm">{{ data.tradeName }}</p>
            <p v-if="data.legalName" class="mt-1 break-words text-sm text-muted-color">{{ data.legalName }}</p>
            <p class="mt-1 text-sm text-muted-color">
              #{{ data.id }} ·
              {{
                data.username === "SIN ASIGNAR"
                  ? "Sin usuario vinculado"
                  : data.username
              }}
            </p>
            <p v-if="data.portalUserMissing" class="mt-1 text-sm text-orange-700 dark:text-orange-300">
              <i class="pi pi-exclamation-triangle" aria-hidden="true" /> Sin acceso al portal: vinculá un usuario.
            </p></template
          ></Column
        >
        <Column field="email" header="Correo" class="hidden md:table-cell" />
        <Column field="phone" header="Teléfono" class="hidden lg:table-cell" />
        <Column header="Acciones"
          ><template #body="{ data }"
            ><div class="flex gap-1">
              <Button
                as="router-link"
                :to="`/clients/${data.id}`"
                label="Ver detalle"
                text
                size="small"
                :aria-label="`Ver detalle de ${data.firstname} ${data.lastname}`"
              /><Button
                as="router-link"
                :to="`/clients/${data.id}/edit`"
                icon="pi pi-pencil"
                text
                size="small"
                :aria-label="`Editar cliente ${data.id}`"
              /></div></template
        ></Column>
      </DataTable>
      <ListPaginator
        :page="page"
        :size="size"
        :total="total"
        @change="paginate"
      /> </template
  ></Card>
</template>
