<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Column from "primevue/column";
import DataTable from "primevue/datatable";
import Message from "primevue/message";
import Select from "primevue/select";
import { useToast } from "primevue/usetoast";
import PageHeader from "../../components/PageHeader.vue";
import ListToolbar from "../../components/ListToolbar.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import LicenseStatus from "../../components/LicenseStatus.vue";
import EntitySelect from "../../components/EntitySelect.vue";
import FormField from "../../components/FormField.vue";
import { usePagedList } from "../../composables/usePagedList";
import { useConfirmAction } from "../../composables/useConfirmAction";
import { request } from "../../services/http";
import { licenseRowClass, lockDateLabel } from "./presentation";
import type { License } from "../../services/admin";
import { clientsApi } from "../clients/api";
const route = useRoute();
const toast = useToast();
const confirm = useConfirmAction();
const status = ref("ALL");
const acting = ref<number>();
const initialClientId = Number(route.query.clientId);
const clientId = ref<number | null>(
  Number.isSafeInteger(initialClientId) && initialClientId > 0
    ? initialClientId
    : null,
);
const clientName = ref("");
const clientError = ref("");
const states = [
  { label: "Todas", value: "ALL" },
  { label: "Activas", value: "ACTIVE" },
  { label: "Por vencer en 15 días", value: "EXPIRING" },
  { label: "Vencidas", value: "EXPIRED" },
  { label: "Bloqueadas", value: "BLOCKED" },
];
const {
  rows,
  keyword,
  page,
  size,
  total,
  loading,
  error,
  search,
  paginate,
  load,
} = usePagedList<License>("/web/api/licenses", () => ({
  status: status.value,
  clientId: clientId.value,
}));
onMounted(async () => {
  const id = clientId.value;
  if (id == null) return;
  try {
    const client = await clientsApi.detail(id);
    if (clientId.value === id)
      clientName.value = `${client.firstname} ${client.lastname}`;
  } catch {
    if (clientId.value === id)
      clientError.value =
        "No se pudo cargar el nombre del cliente seleccionado. Podés buscar otro cliente o limpiar el filtro.";
  }
});
function filterClient(id: number | null) {
  if (id === clientId.value) return;
  clientId.value = id;
  clientName.value = "";
  clientError.value = "";
  void search();
}
async function changeLock(license: License) {
  const locked = !license.locked;
  if (
    !(await confirm(
      `¿${locked ? "Bloquear" : "Desbloquear"} la licencia de ${license.companyName}?`,
    ))
  )
    return;
  acting.value = license.id;
  error.value = "";
  try {
    await request(`/web/api/licenses/${license.id}/lock`, {
      method: "PUT",
      body: JSON.stringify({ locked }),
    });
    toast.add({
      severity: "success",
      summary: "Licencia actualizada",
      life: 3500,
    });
    await load();
  } catch (failure) {
    error.value = (failure as Error).message;
  } finally {
    acting.value = undefined;
  }
}
</script>
<template>
  <PageHeader
    title="Licencias"
    description="Empresas, vigencias y control de acceso de AngryBox."
    ><Button
      as="router-link"
      :to="{ path: '/licenses/new', query: clientId ? { clientId } : {} }"
      label="Nueva licencia"
      icon="pi pi-plus"
  /></PageHeader>
  <Message v-if="clientError" severity="warn" class="mb-4">{{
    clientError
  }}</Message>
  <Message v-if="error" severity="error" class="mb-4">{{ error }}</Message>
  <Card
    ><template #content>
      <ListToolbar
        v-model="keyword"
        :loading="loading"
        placeholder="Empresa o cliente"
        @search="search"
        ><div class="flex w-full flex-wrap items-end gap-3">
          <FormField
            id="license-client-filter"
            label="Cliente"
            class="w-full sm:w-72"
          >
            <EntitySelect
              :model-value="clientId"
              :label="clientName"
              kind="clients"
              input-id="license-client-filter"
              @update:model-value="filterClient"
            />
          </FormField>
          <Button
            v-if="clientId"
            type="button"
            label="Limpiar cliente"
            icon="pi pi-filter-slash"
            text
            severity="secondary"
            @click="filterClient(null)"
          />
          <FormField id="license-status-filter" label="Estado">
            <Select
              input-id="license-status-filter"
              v-model="status"
              :options="states"
              option-label="label"
              option-value="value"
              aria-label="Estado de licencia"
              @change="search"
          /></FormField></div
      ></ListToolbar>
      <DataTable
        :value="rows"
        :loading="loading"
        data-key="id"
        row-hover
        :row-class="licenseRowClass"
        striped-rows
        scrollable
      >
        <template #empty
          >No se encontraron licencias con estos filtros.</template
        >
        <Column header="Empresa"
          ><template #body="{ data }"
            ><span class="font-medium">{{ data.companyName }}</span>
            <p class="mt-1 text-sm text-muted-color">
              Empresa #{{ data.companyId }} · {{ data.clientName }}
            </p></template
          ></Column
        >
        <Column header="Fecha de bloqueo"
          ><template #body="{ data }"
            ><span>{{ lockDateLabel(data) }}</span>
            <p class="mt-1 text-sm text-muted-color">
              {{ data.prePayment ? "Pago adelantado" : "Mes vencido" }}
            </p></template
          ></Column
        >
        <Column header="Estado"
          ><template #body="{ data }"
            ><LicenseStatus :status="data.status" /></template
        ></Column>
        <Column header="Acciones"
          ><template #body="{ data }"
            ><div class="flex flex-wrap gap-1">
              <Button
                as="router-link"
                :to="`/licenses/${data.id}/edit`"
                label="Editar"
                icon="pi pi-pencil"
                text
                size="small"
              /><Button
                as="router-link"
                :to="{
                  path: '/payments',
                  query: { licenseId: data.id, companyName: data.companyName },
                }"
                label="Pagos"
                icon="pi pi-wallet"
                text
                size="small"
              /><Button
                :label="data.locked ? 'Desbloquear' : 'Bloquear'"
                :severity="data.locked ? 'secondary' : 'danger'"
                text
                size="small"
                :loading="acting === data.id"
                :disabled="acting != null"
                @click="changeLock(data)"
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
