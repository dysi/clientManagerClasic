<script setup lang="ts">
import { ref } from "vue";
import Button from "primevue/button";
import Card from "primevue/card";
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import Select from "primevue/select";
import Tag from "primevue/tag";
import Message from "primevue/message";
import PageHeader from "../../components/PageHeader.vue";
import ListToolbar from "../../components/ListToolbar.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import { usePagedList } from "../../composables/usePagedList";
import { parameterStates, type Bank } from "../../services/parameters";
import { useParameterActivation } from "./useParameterActivation";
const status = ref("ALL");
const { rows, keyword, loading, error, page, size, total, load, search, paginate } =
  usePagedList<Bank>("/web/api/banks", () => ({ status: status.value }));
const { acting, change } = useParameterActivation("banks", error, load);
</script>
<template>
  <PageHeader title="Bancos" description="Desactivar un banco deja sus cuentas fuera de las opciones para nuevos pagos.">
    <Button as="router-link" to="/parameters" label="Volver a parámetros" text icon="pi pi-arrow-left" />
    <Button as="router-link" to="/parameters/banks/new" label="Nuevo banco" icon="pi pi-plus" />
  </PageHeader>

  <Message v-if="error" severity="error" class="mb-4">{{ error }}</Message>
  <Card><template #content>
    <ListToolbar v-model="keyword" :loading="loading" placeholder="Nombre del banco" @search="search">
      <Select v-model="status" :options="parameterStates" option-label="label" option-value="value" aria-label="Estado" @change="search" />
    </ListToolbar>
    <DataTable :value="rows" :loading="loading" data-key="id" row-hover striped-rows scrollable>
      <template #empty>No se encontraron bancos con estos filtros.</template>
      <Column field="name" header="Banco" />
      <Column header="Estado"><template #body="{ data }">
        <Tag :value="data.active ? 'Activo' : 'Inactivo'" :severity="data.active ? 'success' : 'secondary'" />

      </template></Column>
      <Column header="Acciones"><template #body="{ data }">
        <div class="flex flex-wrap gap-1">
          <Button as="router-link" :to="'/parameters/banks/' + data.id + '/edit'" label="Editar" text size="small" :aria-label="'Editar banco ' + data.name" />
          <Button as="router-link" :to="{ path: '/parameters/bank-accounts', query: { bankId: data.id, bankName: data.name } }" label="Cuentas" text size="small" />
          <Button type="button" :label="data.active ? 'Desactivar' : 'Activar'" :severity="data.active ? 'danger' : 'secondary'" text size="small"
            :loading="acting === data.id" :disabled="acting != null" @click="change(data.id, !data.active, data.name)" />
        </div>
      </template></Column>
    </DataTable>
    <ListPaginator :page="page" :size="size" :total="total" @change="paginate" />
  </template></Card>
</template>
