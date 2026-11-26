<script setup lang="ts">
import { ref } from "vue";
import { useRoute } from "vue-router";
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
import { parameterStates, type BankAccount } from "../../services/parameters";
import { useParameterActivation } from "./useParameterActivation";
const route = useRoute();
const bankId = route.query.bankId ? Number(route.query.bankId) : undefined;
const status = ref("ALL");
const { rows, keyword, loading, error, page, size, total, load, search, paginate } =
  usePagedList<BankAccount>("/web/api/bank-accounts", () => ({ status: status.value, bankId }));
const { acting, change } = useParameterActivation("bank-accounts", error, load);
</script>
<template>
  <PageHeader title="Cuentas bancarias" description="Cuentas destinatarias de transferencias. Los datos históricos de pagos se conservan.">
    <Button as="router-link" to="/parameters" label="Volver a parámetros" text icon="pi pi-arrow-left" />
    <Button as="router-link" to="/parameters/bank-accounts/new" label="Nueva cuenta" icon="pi pi-plus" />
  </PageHeader>
  <Message v-if="bankId" severity="info" class="mb-4">Cuentas de {{ route.query.bankName || "el banco #" + bankId }}. <RouterLink to="/parameters/bank-accounts" class="underline">Ver todas</RouterLink></Message>
  <Message v-if="error" severity="error" class="mb-4">{{ error }}</Message>
  <Card><template #content>
    <ListToolbar v-model="keyword" :loading="loading" placeholder="Banco, titular, CBU, alias o código externo" @search="search">
      <Select v-model="status" :options="parameterStates" option-label="label" option-value="value" aria-label="Estado" @change="search" />
    </ListToolbar>
    <DataTable :value="rows" :loading="loading" data-key="id" row-hover striped-rows scrollable>
      <template #empty>No se encontraron cuentas bancarias con estos filtros.</template>
      <Column field="bankName" header="Banco" />
      <Column header="Código externo"><template #body="{ data }">{{ data.externalCode || "Sin código" }}</template></Column>
      <Column field="holder" header="Titular" />
      <Column header="CBU y alias"><template #body="{ data }">
        <span class="whitespace-nowrap font-mono">{{ data.cbu }}</span>
        <p class="mt-1 text-sm text-muted-color">{{ data.alias || 'Sin alias' }}</p>
      </template></Column>
      <Column header="Estado"><template #body="{ data }">
        <Tag :value="data.active ? 'Activa' : 'Inactiva'" :severity="data.active ? 'success' : 'secondary'" />
        <p class="mt-1 text-sm text-muted-color">{{ data.available ? "Disponible para transferencias" : !data.bankActive ? "No disponible: banco inactivo" : "No disponible: cuenta inactiva" }}</p>
      </template></Column>
      <Column header="Acciones"><template #body="{ data }">
        <div class="flex flex-wrap gap-1">
          <Button as="router-link" :to="'/parameters/bank-accounts/' + data.id + '/edit'" label="Editar" text size="small" :aria-label="'Editar cuenta ' + data.cbu" />

          <Button type="button" :label="data.active ? 'Desactivar' : 'Activar'" :severity="data.active ? 'danger' : 'secondary'" text size="small"
            :loading="acting === data.id" :disabled="acting != null" @click="change(data.id, !data.active, data.cbu)" />
        </div>
      </template></Column>
    </DataTable>
    <ListPaginator :page="page" :size="size" :total="total" @change="paginate" />
  </template></Card>
</template>
