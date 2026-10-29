<script setup lang="ts">
import { ref } from "vue";
import Select from "primevue/select";
import { money, displayDateTime } from "../../services/format";
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
const { rows, keyword, loading, error, page, size, total, search, paginate } =
  usePagedList<Client>("/web/api/clients", () => ({ balanceState: balanceState.value, staleDays: staleDays.value || null }));
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
      <div class="mb-5 grid gap-4 sm:grid-cols-2">
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
      </div>
      <ListPaginator
        :page="page"
        :size="size"
        :total="total"
        @change="paginate"
      /> </template
  ></Card>
</template>
