<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Column from "primevue/column";
import DataTable from "primevue/datatable";
import Dialog from "primevue/dialog";
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
import LicenseEditor from "./LicenseEditor.vue";
import LicenseBatchDialog from "./LicenseBatchDialog.vue";
const route = useRoute();
const toast = useToast();
const confirm = useConfirmAction();
const status = ref("ALL");
const clientStatus = ref("ACTIVE");
const visibility = ref("VISIBLE");
const clientStates = [
  { label: "Todos", value: "ALL" }, { label: "Activos", value: "ACTIVE" },
  { label: "Dados de baja", value: "REMOVED" },
];
const visibilityOptions = [
  { label: "Todos", value: "ALL" }, { label: "Visibles", value: "VISIBLE" },
  { label: "Ocultos", value: "HIDDEN" },
];
const acting = ref<number>();
const editingId = ref<number>();
const editor = ref<InstanceType<typeof LicenseEditor> | null>(null);
const selectionMode = ref(false);
const selected = ref<License[]>([]);
const batchSelection = ref<License[] | null>(null);
function toggleSelection() {
  selectionMode.value = !selectionMode.value;
  selected.value = [];
}
function batchSaved(updated: License[]) {
  const byId = new Map(updated.map(license => [license.id, license]));
  rows.value = rows.value.map(row => byId.get(row.id) ?? row);
  selected.value = [];
  batchSelection.value = null;
}
function licenseSaved(license: License) {
  // Conserva página, filtros y filas: los estados calculados vienen del guardado.
  rows.value = rows.value.map(row => row.id === license.id ? license : row);
  selected.value = [];
  editingId.value = undefined;
}
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
  clientStatus: clientStatus.value,
  visibility: visibility.value,
  clientId: clientId.value,
}));
// Una selección nunca se traslada silenciosamente a otra búsqueda o página.
watch(loading, value => { if (value) selected.value = []; }, { flush: "sync" });
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
  if (acting.value != null) return;
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
async function changeVisibility(license: License) {
  if (acting.value != null) return;
  const hidden = !license.hidden;
  if (!(await confirm(`¿${hidden ? "Ocultar" : "Hacer visible"} la licencia de ${license.companyName}?${hidden ? " Dejará de aparecer en el portal y en el listado de visibles." : ""}`))) return;
  acting.value = license.id;
  error.value = "";
  try {
    await request(`/web/api/licenses/${license.id}/visibility`, { method: "PUT", body: JSON.stringify({ hidden }) });
    await load();
    toast.add({ severity: "success", summary: "Visibilidad actualizada", life: 3500 });
  } catch (failure) {
    error.value = failure instanceof Error ? failure.message : "No se pudo actualizar la visibilidad.";
  } finally { acting.value = undefined; }
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
          <FormField id="license-client-status" label="Estado Cliente">
            <Select aria-label="Estado Cliente" input-id="license-client-status" v-model="clientStatus" :options="clientStates"
              option-label="label" option-value="value" @change="search" />
          </FormField>
          <FormField id="license-visibility" label="Visibilidad">
            <Select aria-label="Visibilidad" input-id="license-visibility" v-model="visibility" :options="visibilityOptions"
              option-label="label" option-value="value" @change="search" />
          </FormField>
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
      <div class="mb-4 flex flex-wrap items-center gap-3">
        <Button type="button" :label="selectionMode ? 'Cancelar selección' : 'Seleccionar licencias'"
          icon="pi pi-check-square" severity="secondary" outlined :disabled="loading || acting != null"
          @click="toggleSelection" />
        <template v-if="selectionMode">
          <span class="text-sm" role="status">{{ selected.length }} seleccionadas de {{ rows.length }} en esta página</span>
          <Button type="button" label="Modificar seleccionados" icon="pi pi-pencil"
            :disabled="!selected.length || loading || acting != null"
            @click="batchSelection = selected.map(license => ({ ...license }))" />
          <p class="w-full text-sm text-muted-color">La casilla de la cabecera selecciona todas las licencias de esta página. Cambiar de búsqueda o página limpia la selección.</p>
        </template>
      </div>
      <div class="min-w-0 overflow-x-auto">
      <DataTable
        v-model:selection="selected"
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
        <Column v-if="selectionMode" selection-mode="multiple" header-style="width: 3rem" />
        <Column header="Empresa"
          ><template #body="{ data }"
            ><span class="font-medium">{{ data.companyName }}</span>
            <p class="mt-1 text-sm text-muted-color">
              Empresa #{{ data.companyId }} ·
              <RouterLink
                :to="`/clients/${data.clientId}`"
                class="text-primary underline"
                :aria-label="`Ver cliente ${data.clientName}`"
              >{{ data.clientName }}</RouterLink>
            </p>
            <p v-if="data.clientRemoved" class="mt-1 text-sm font-medium text-red-700 dark:text-red-300">Cliente dado de baja: bloqueo indirecto de la licencia.</p>
            <p v-if="data.hidden" class="mt-1 text-sm text-muted-color">Oculta en el portal</p></template
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
                type="button"
                @click="editingId = data.id"
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
              /><Button as="router-link" :to="{ path: '/license-query-log', query: { companyId: data.companyId } }"
                label="Bitácora" icon="pi pi-history" text size="small"
              /><Button
                :label="data.locked ? 'Desbloquear' : 'Bloquear'"
                :severity="data.locked ? 'secondary' : 'danger'"
                text
                size="small"
                :loading="acting === data.id"
                :disabled="acting != null"
                @click="changeLock(data)"
              /><Button v-if="data.locked || data.hidden" type="button"
                :label="data.hidden ? 'Hacer visible' : 'Ocultar'" :icon="data.hidden ? 'pi pi-eye' : 'pi pi-eye-slash'"
                text size="small" :loading="acting === data.id" :disabled="acting != null"
                @click="changeVisibility(data)" /></div></template
        ></Column> </DataTable
      ></div><ListPaginator
        :page="page"
        :size="size"
        :total="total"
        @change="paginate"
      /> </template
  ></Card>
  <Dialog v-if="editingId != null" :visible="true" modal header="Editar licencia"
    class="w-[min(64rem,calc(100vw-2rem))]" :draggable="false"
    :closable="!editor?.saving" :close-on-escape="!editor?.saving"
    @update:visible="!$event && editor?.cancel()">
    <LicenseEditor :key="editingId" ref="editor" :id="editingId"
      @saved="licenseSaved" @cancel="editingId = undefined" />
  </Dialog>
  <LicenseBatchDialog v-if="batchSelection" :licenses="batchSelection"
    @close="batchSelection = null" @saved="batchSaved" />
</template>
