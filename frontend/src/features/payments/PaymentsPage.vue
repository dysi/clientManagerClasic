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
import { useToast } from "primevue/usetoast";
import PageHeader from "../../components/PageHeader.vue";
import ListToolbar from "../../components/ListToolbar.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import { useConfirmAction } from "../../composables/useConfirmAction";
import { usePagedList } from "../../composables/usePagedList";
import { request } from "../../services/http";
import { money, displayDate } from "../../services/format";
import type { Payment } from "../../services/admin";
const route = useRoute();
const toast = useToast();
const confirm = useConfirmAction();
const licenseId = route.query.licenseId
  ? Number(route.query.licenseId)
  : undefined;
const status = ref("ALL");
const acting = ref<number>();
const states = [
  { label: "Todos", value: "ALL" },
  { label: "Pendientes", value: "PENDING" },
  { label: "Pagados", value: "PAID" },
  { label: "Anulados", value: "REMOVED" },
];
const {
  rows,
  keyword,
  loading,
  error,
  page,
  size,
  total,
  load,
  search,
  paginate,
} = usePagedList<Payment>("/web/api/payments", () => ({
  status: status.value,
  licenseId,
}));
async function changeStatus(payment: Payment) {
  const removed = !payment.removed;
  if (
    !(await confirm(
      `¿${removed ? "Anular" : "Reactivar"} el pago #${payment.id} de ${payment.companyName}?`,
    ))
  )
    return;
  acting.value = payment.id;
  error.value = "";
  try {
    await request(`/web/api/payments/${payment.id}/status`, {
      method: "PUT",
      body: JSON.stringify({ removed }),
    });
    toast.add({
      severity: "success",
      summary: removed ? "Pago anulado" : "Pago reactivado",
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
    title="Pagos"
    description="Registro de importes, vencimientos y cobros por licencia."
    ><Button
      as="router-link"
      :to="{ path: '/payments/new', query: licenseId ? { licenseId } : {} }"
      label="Nuevo pago"
      icon="pi pi-plus"
  /></PageHeader>
  <Message v-if="licenseId" severity="info" class="mb-4"
    >Pagos de {{ route.query.companyName || `la licencia #${licenseId}` }}.
    <RouterLink to="/payments" class="underline">Ver todos</RouterLink></Message
  >
  <Message v-if="error" severity="error" class="mb-4">{{ error }}</Message>
  <Card
    ><template #content>
      <ListToolbar
        v-model="keyword"
        :loading="loading"
        placeholder="Empresa u observación"
        @search="search"
        ><Select
          v-model="status"
          :options="states"
          option-label="label"
          option-value="value"
          aria-label="Estado del pago"
          @change="search"
      /></ListToolbar>
      <DataTable
        :value="rows"
        :loading="loading"
        data-key="id"
        striped-rows
        scrollable
      >
        <template #empty>No se encontraron pagos con estos filtros.</template>
        <Column header="Empresa / período"
          ><template #body="{ data }"
            ><span class="font-medium">{{ data.companyName }}</span>
            <p class="mt-1 text-sm text-muted-color">
              #{{ data.id }} · {{ displayDate(data.paymentDate) }}
            </p></template
          ></Column
        >
        <Column header="Importe neto"
          ><template #body="{ data }"
            >{{ money(data.amount - data.discount) }}
            <p v-if="data.discount" class="mt-1 text-sm text-muted-color">
              Descuento: {{ money(data.discount) }}
            </p></template
          ></Column
        >
        <Column header="Fechas"
          ><template #body="{ data }"
            ><p class="text-sm">
              Vence: {{ displayDate(data.expirationDate) }}
            </p>
            <p class="mt-1 text-sm text-muted-color">
              Cobrado: {{ displayDate(data.paymentRealDate) }}
            </p></template
          ></Column
        >
        <Column header="Estado"
          ><template #body="{ data }"
            ><Tag
              :value="
                data.removed
                  ? 'Anulado'
                  : data.paymentRealDate
                    ? 'Pagado'
                    : 'Pendiente'
              "
              :severity="
                data.removed
                  ? 'secondary'
                  : data.paymentRealDate
                    ? 'success'
                    : 'warn'
              " /></template
        ></Column>
        <Column header="Acciones"
          ><template #body="{ data }"
            ><div class="flex flex-wrap gap-1">
              <Button
                as="router-link"
                :to="`/payments/${data.id}/edit`"
                label="Editar"
                text
                size="small"
                :disabled="data.removed"
              /><Button
                :label="data.removed ? 'Reactivar' : 'Anular'"
                :severity="data.removed ? 'secondary' : 'danger'"
                text
                size="small"
                :loading="acting === data.id"
                :disabled="acting != null"
                @click="changeStatus(data)"
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
