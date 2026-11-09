<script setup lang="ts">
import { ref } from "vue";
import Select from "primevue/select";
import { money, displayDateTime } from "../../services/format";
import Button from "primevue/button";
import Card from "primevue/card";
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import Message from "primevue/message";
import Tag from "primevue/tag";
import PageHeader from "../../components/PageHeader.vue";
import ListToolbar from "../../components/ListToolbar.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import { usePagedList } from "../../composables/usePagedList";
import type { Client } from "./api";
import { useConfirmAction } from "../../composables/useConfirmAction";
import { request } from "../../services/http";
const confirm = useConfirmAction();
const acting = ref<number>();
const status = ref("ACTIVE");
const statusOptions = [
  { label: "Todos", value: "ALL" },
  { label: "Activos", value: "ACTIVE" },
  { label: "Dados de baja", value: "REMOVED" },
];
const balanceState = ref("ALL");
const staleDays = ref(0);
const balanceOptions = [
  { label: "Todos los saldos", value: "ALL" },
  { label: "Con deuda", value: "DEBT" },
  { label: "Saldo a favor", value: "CREDIT" },
  { label: "Saldo cero", value: "ZERO" },
  { label: "Sin informar", value: "UNKNOWN" },
];
const syncOptions = [
  { label: "Cualquier fecha", value: 0 },
  ...[15, 30, 45, 60].map(value => ({ label: `Sin sincronizar hace más de ${value} días`, value })),
];
const { rows, keyword, loading, error, page, size, total, search, paginate, load } =
  usePagedList<Client>("/web/api/clients", () => ({ status: status.value, balanceState: balanceState.value, staleDays: staleDays.value || null }));
function clientRowClass(client: Client) {
  return client.removed ? "bg-red-100! hover:bg-red-200!" : undefined;
}
async function changeStatus(client: Client) {
  if (acting.value != null) return;
  const removed = !client.removed;
  if (!(await confirm(removed
    ? `¿Dar de baja a ${client.firstname} ${client.lastname}? Todas sus licencias quedarán bloqueadas indirectamente. El acceso al portal se conserva.`
    : `¿Dar de alta a ${client.firstname} ${client.lastname}? Sus licencias conservarán sus bloqueos y vencimientos propios.`))) return;
  acting.value = client.id;
  error.value = "";
  try {
    await request(`/web/api/clients/${client.id}/status`, { method: "PUT", body: JSON.stringify({ removed }) });
    await load();
  } catch (failure) {
    error.value = failure instanceof Error ? failure.message : "No se pudo actualizar el cliente.";
  } finally { acting.value = undefined; }
}
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
      <div class="mb-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div class="flex flex-col gap-2">
          <label for="client-status">Estado</label>
          <Select aria-label="Estado" input-id="client-status" v-model="status" :options="statusOptions"
            option-label="label" option-value="value" @change="search" />
        </div>
        <div class="flex flex-col gap-2">
          <label for="balance-state">Saldo del cliente</label>
          <Select aria-label="Saldo del cliente" input-id="balance-state" v-model="balanceState" :options="balanceOptions"
            option-label="label" option-value="value" @change="search" />
        </div>
        <div class="flex flex-col gap-2">
          <label for="balance-sync">Última sincronización del saldo</label>
          <Select aria-label="Última sincronización del saldo" input-id="balance-sync" v-model="staleDays" :options="syncOptions"
            option-label="label" option-value="value" @change="search" />
        </div>
      </div>
      <p v-if="staleDays" class="mb-4 text-sm text-muted-color">
        Solo saldos informados con más de {{ staleDays }} días sin sincronizar. Combiná con «Con deuda» para revisar deudas.
      </p>
      <div class="min-w-0 overflow-x-auto">
      <DataTable
        :value="rows"
        :table-style="{ minWidth: '48rem' }"
        :loading="loading"
        data-key="id"
        row-hover
        :row-class="clientRowClass"
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
        <Column header="Estado">
          <template #body="{ data }">
            <Tag :value="data.removed ? 'Dado de baja' : 'Activo'"
              :severity="data.removed ? 'danger' : 'success'" />
          </template>
        </Column>
        <Column header="CUIT/DNI"><template #body="{ data }">
          <p v-if="data.cuil">CUIT: {{ data.cuil }}</p><p v-if="data.dni">DNI: {{ data.dni }}</p>
          <span v-if="!data.cuil && !data.dni">—</span>
        </template></Column>
        <Column header="Dirección"><template #body="{ data }">
          <div class="min-w-48 max-w-80 whitespace-normal break-words">
            <p v-if="data.street || data.streetNumber">{{ [data.street, data.streetNumber].filter(Boolean).join(' ') }}</p>
            <p v-if="data.addressComplement">{{ data.addressComplement }}</p>
            <p v-if="data.neighborhood">Barrio / Sector: {{ data.neighborhood }}</p>
            <p>{{ [data.city, data.provinceName, data.countryName].filter(Boolean).join(', ') }}</p>
            <p v-if="data.postalCode">CP: {{ data.postalCode }}</p>
          </div>
        </template></Column>
        <Column header="Observaciones"><template #body="{ data }">
          <p v-if="data.observations" class="min-w-48 max-w-80 whitespace-pre-wrap break-words">{{ data.observations }}</p>
        </template></Column>
        <Column field="email" header="Correo" class="hidden md:table-cell" />
        <Column field="phone" header="Teléfono" class="hidden lg:table-cell" />
        <Column header="Deuda / saldo">
          <template #body="{ data }">
            <template v-if="data.accountBalance">
              <p class="whitespace-nowrap font-medium">{{ money(data.accountBalance.amount) }}</p>
              <p class="text-sm text-muted-color">{{ data.accountBalance.amount > 0 ? "Deuda" : data.accountBalance.amount < 0 ? "Saldo a favor" : "Sin deuda" }}</p>
              <p class="mt-1 whitespace-nowrap text-sm text-muted-color">
                Sincronizado: {{ displayDateTime(data.accountBalance.synchronizedAt) }}
              </p>
            </template>
            <span v-else class="text-muted-color">Sin informar</span>
          </template>
        </Column>
        <Column header="Acciones"
          ><template #body="{ data }"
            ><div class="flex flex-wrap gap-1">
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
              /><Button type="button" :label="data.removed ? 'Dar de alta' : 'Dar de baja'"
                :severity="data.removed ? 'secondary' : 'danger'" text size="small"
                :loading="acting === data.id" :disabled="acting != null" @click="changeStatus(data)" /></div></template
        ></Column>
      </DataTable>
      </div>
      <ListPaginator
        :page="page"
        :size="size"
        :total="total"
        @change="paginate"
      /> </template
  ></Card>
</template>
