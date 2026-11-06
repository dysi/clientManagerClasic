<script setup lang="ts">
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import Message from "primevue/message";
import ListToolbar from "../../components/ListToolbar.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import LicenseStatus from "../../components/LicenseStatus.vue";
import { usePagedList } from "../../composables/usePagedList";
import type { PortalLicense } from "../../services/portal";
import { licenseRowClass } from "../licenses/presentation";

const { rows, keyword, loading, error, page, size, total, search, paginate } = usePagedList<PortalLicense>("/web/portal/licenses");
</script>
<template>
  <div>
      <Message v-if="error" severity="error" role="alert" class="mb-4">{{ error }}</Message>
      <ListToolbar v-model="keyword" :loading="loading" placeholder="Buscar por empresa" @search="search" />
      <DataTable :value="rows" :loading="loading" data-key="id" :row-class="licenseRowClass" scrollable row-hover>
        <template #empty>No tenés licencias registradas con esta búsqueda.</template>
        <Column field="companyName" header="Empresa" />
        <Column header="Estado"><template #body="{ data }"><LicenseStatus :status="data.status" /></template></Column>
      </DataTable>
      <ListPaginator :page="page" :size="size" :total="total" @change="paginate" />
  </div>
</template>
