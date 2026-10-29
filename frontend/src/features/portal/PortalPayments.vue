<script setup lang="ts">
import Button from "primevue/button";
import Card from "primevue/card";
import Column from "primevue/column";
import DataTable from "primevue/datatable";
import Message from "primevue/message";
import Tag from "primevue/tag";
import ListPaginator from "../../components/ListPaginator.vue";
import { usePagedList } from "../../composables/usePagedList";
import { displayDateTime, money } from "../../services/format";
import type { PortalPayment } from "../../services/portal";
defineProps<{ sensitiveAccess: boolean }>();
const { rows, loading, error, page, size, total, paginate } = usePagedList<PortalPayment>("/web/portal/payments");
const labels = { PENDIENTE: "Pendiente", APROBADO: "Aprobado", RECHAZADO: "Rechazado" };
</script>
<template>
  <Card>
    <template #title><div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <span>Mis pagos informados <span class="text-base font-normal text-muted-color">(últimos 45 días)</span></span>
      <Button as="router-link" to="/portal/payments/new" label="Informar un nuevo pago" icon="pi pi-plus-circle" size="large" class="w-full sm:w-auto" />
    </div></template>
    <template #subtitle>Informar un pago no modifica automáticamente tu saldo ni la vigencia de tus licencias.</template>
    <template #content>
      <Message v-if="error" severity="error" role="alert" class="mb-4">{{ error }}</Message>
      <DataTable :value="rows" :loading="loading" data-key="id" scrollable class="mt-4">
        <template #empty>Todavía no informaste pagos.</template>
        <Column header="Pago y estado"><template #body="{ data }: { data: PortalPayment }">
          <div class="flex flex-col items-start gap-2">
            <span>#{{ data.id }}</span>
            <Tag :value="labels[data.status]" :severity="data.status === 'APROBADO' ? 'success' : data.status === 'RECHAZADO' ? 'danger' : 'warn'" />
            <p v-if="data.rejectionReason" class="max-w-sm whitespace-pre-wrap break-words text-sm">{{ data.rejectionReason }}</p>
          </div>
        </template></Column>
        <Column header="Emisión"><template #body="{ data }">{{ displayDateTime(data.issuedAt) }}</template></Column>
        <Column header="Importe"><template #body="{ data }">{{ money(data.amount) }}</template></Column>
        <Column header="Licencia"><template #body="{ data }">{{ data.companyName || 'Sin licencia asociada' }}</template></Column>
        <Column header="Comentarios"><template #body="{ data }">
          <span :aria-label="`${data.commentCount} comentarios públicos`">{{ data.commentCount }}</span>
        </template></Column>
        <Column header="Archivos Adjuntos" field="receiptCount" />
        <Column v-if="sensitiveAccess" header="Detalle"><template #body="{ data }">
          <Button as="router-link" :to="`/portal/payments/${data.id}`" label="Ver detalle" icon="pi pi-eye" text size="small" />
        </template></Column>
      </DataTable>
      <ListPaginator :page="page" :size="size" :total="total" @change="paginate" />
    </template>
  </Card>
</template>
