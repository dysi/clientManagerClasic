<script setup lang="ts">
import { ref } from "vue";
import Select from "primevue/select";
import Button from "primevue/button";
import Card from "primevue/card";
import Skeleton from "primevue/skeleton";
import ClientCard from "./ClientCard.vue";
import Message from "primevue/message";
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
        full-width
        placeholder="Nombre, razón social, fantasía, correo, CUIL, DNI o usuario"
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
    </template>
  </Card>
  <section class="mt-6 min-w-0" aria-label="Resultados de clientes" :aria-busy="loading">
    <p class="mb-4 text-sm text-muted-color" role="status">
      <template v-if="loading">Buscando clientes…</template>
      <template v-else>{{ total }} {{ total === 1 ? 'cliente encontrado' : 'clientes encontrados' }} · Todos los datos disponibles en cada tarjeta.</template>
    </p>
    <div v-if="loading" class="space-y-5" aria-hidden="true">
      <Card v-for="item in 3" :key="item"><template #content>
        <Skeleton width="45%" height="1.75rem" class="mb-5" />
        <Skeleton height="8rem" />
      </template></Card>
    </div>
    <Message v-else-if="!rows.length && !error" severity="info" :closable="false">
      No se encontraron clientes. Probá otra búsqueda o creá un cliente.
    </Message>
    <div v-else-if="!loading" class="space-y-5">
      <ClientCard v-for="client in rows" :key="client.id" :client="client"
        :acting="acting === client.id" :actions-disabled="acting != null"
        @change-status="changeStatus(client)" />
    </div>
    <Button v-if="error" type="button" label="Reintentar búsqueda" icon="pi pi-refresh"
      outlined class="mt-4" :disabled="loading" @click="load" />
    <ListPaginator v-if="!loading && total > 0" class="mt-5"
      :page="page" :size="size" :total="total" @change="paginate" />
  </section>
</template>
