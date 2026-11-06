<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Message from "primevue/message";
import Skeleton from "primevue/skeleton";
import Tag from "primevue/tag";
import PageHeader from "../../components/PageHeader.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import { usePagedList } from "../../composables/usePagedList";
import { useDraft } from "../../composables/useDraft";
import { request } from "../../services/http";
import { displayDateTime, money } from "../../services/format";
import { appendReceipts, receiptUrl, type PortalPaymentDetail, type PortalReceipt, type PaymentFormContext, type ReceiptSelection } from "../../services/portal";
import ReceiptPicker from "./ReceiptPicker.vue";
import PortalCommentsPage from "./PortalCommentsPage.vue";
const id = Number(useRoute().params.id);
const detail = ref<PortalPaymentDetail | null>(null);
const limits = ref<PaymentFormContext | null>(null);
const loading = ref(true);
const loadError = ref("");
const receipts = ref<ReceiptSelection[]>([]);
const draft = ref({ files: [] as string[], descriptions: [] as string[] });
const { saving, error, save, reset } = useDraft(draft);
const validation = ref("");
const key = () => Array.from(crypto.getRandomValues(new Uint8Array(16)), value => value.toString(16).padStart(2, "0")).join("");
const uploadKey = ref(key());
const { rows, loading: loadingReceipts, error: receiptsError, page, size, total, paginate, load: reloadReceipts } = usePagedList<PortalReceipt>(`/web/portal/payments/${id}/attachments`);
const labels = { PENDIENTE: "Pendiente de revisión", APROBADO: "Aprobado", RECHAZADO: "Rechazado" };
onMounted(async () => {
  try {
    const [payment, context] = await Promise.all([request<PortalPaymentDetail>(`/web/portal/payments/${id}`), request<PaymentFormContext>("/web/portal/payment-form")]);
    detail.value = payment; limits.value = context;
  } catch (failure) { loadError.value = failure instanceof Error ? failure.message : "No se pudo abrir el pago."; }
  finally { loading.value = false; }
});
function select(value: ReceiptSelection[]) {
  receipts.value = value;
  draft.value = { files: value.map(item => JSON.stringify([item.file.name, item.file.size, item.file.lastModified])), descriptions: value.map(item => item.description) };
}
async function submit() {
  validation.value = "";
  if (!limits.value || !receipts.value.length || receipts.value.length > limits.value.maxReceipts
      || receipts.value.some(item => item.file.size === 0 || item.file.size > (limits.value?.maxFileBytes ?? 0))) {
    validation.value = "Revisá la cantidad y el tamaño de los archivos antes de enviarlos."; return;
  }
  const result = await save(() => appendReceipts(id, uploadKey.value, receipts.value), "Se agregaron los comprobantes al pago.");
  if (result) { select([]); reset(); uploadKey.value = key(); await reloadReceipts(); }
}
</script>
<template>
  <PageHeader :title="'Detalle del pago #' + id" description="Consultá tu presentación, sus comprobantes y comentarios.">
    <Button as="router-link" to="/portal" label="Volver al portal" icon="pi pi-arrow-left" text />
  </PageHeader>
  <Skeleton v-if="loading" height="16rem" />
  <Message v-else-if="loadError" severity="error" role="alert">{{ loadError }}</Message>
  <div v-else-if="detail" class="flex min-w-0 flex-col gap-6">
    <Card><template #title><div class="flex flex-wrap items-center gap-3"><span>{{ money(detail.payment.amount) }}</span>
      <Tag :value="labels[detail.payment.status]" :severity="detail.payment.status === 'APROBADO' ? 'success' : detail.payment.status === 'RECHAZADO' ? 'danger' : 'warn'" /></div></template>
      <template #content>
        <Message v-if="detail.payment.rejectionReason" severity="error" :closable="false" class="mb-5"><span class="whitespace-pre-wrap break-words">{{ detail.payment.rejectionReason }}</span></Message>
        <dl class="grid min-w-0 gap-5 sm:grid-cols-2">
          <div><dt class="text-sm text-muted-color">Emisión</dt><dd>{{ displayDateTime(detail.payment.issuedAt) }}</dd></div>
          <div><dt class="text-sm text-muted-color">Forma de pago</dt><dd>Transferencia · {{ detail.payment.currency }}</dd></div>
          <div><dt class="text-sm text-muted-color">Teléfono informado</dt><dd class="break-words">{{ detail.contactPhone }}</dd></div>
          <div><dt class="text-sm text-muted-color">Licencia</dt><dd class="break-words">{{ detail.payment.companyName || 'Sin licencia asociada' }}<span v-if="detail.licenseId"> (#{{ detail.licenseId }})</span></dd></div>
          <div><dt class="text-sm text-muted-color">Banco destinatario</dt><dd class="break-words">{{ detail.destinationBank }}</dd></div>
          <div><dt class="text-sm text-muted-color">Titular</dt><dd class="break-words">{{ detail.destinationHolder }}</dd></div>
          <div><dt class="text-sm text-muted-color">CBU</dt><dd class="break-all font-mono">{{ detail.destinationCbu }}</dd></div>
          <div><dt class="text-sm text-muted-color">Alias</dt><dd class="break-all">{{ detail.destinationAlias || 'Sin alias' }}</dd></div>
          <div v-if="detail.withoutReceipt" class="sm:col-span-2"><dt class="text-sm text-muted-color">Presentación original sin comprobantes</dt><dd class="whitespace-pre-wrap break-words">{{ detail.noReceiptReason }}</dd></div>
        </dl>
      </template>
    </Card>
    <Card><template #title>Comprobantes adjuntos ({{ total }})</template><template #content>
      <Message v-if="receiptsError" severity="error" role="alert">{{ receiptsError }}</Message>
      <Skeleton v-if="loadingReceipts" height="6rem" />
      <p v-else-if="!rows.length && !receiptsError" class="text-muted-color">Todavía no hay comprobantes adjuntos.</p>
      <ul v-else class="flex flex-col gap-5">
        <li v-for="file in rows" :key="file.id" class="min-w-0 border-b border-surface pb-4">
          <p class="break-all font-medium">{{ file.originalName }}</p>
          <p class="text-sm text-muted-color">{{ displayDateTime(file.uploadedAt) }} · {{ (file.size / 1048576).toFixed(2) }} MB</p>
          <p v-if="file.description" class="mt-2 whitespace-pre-wrap break-words">{{ file.description }}</p>
          <div class="mt-2 flex flex-wrap gap-2">
            <Button as="a" :href="receiptUrl(id, file.id)" target="_blank" rel="noopener noreferrer" label="Abrir archivo" icon="pi pi-external-link" text size="small" />
            <Button as="a" :href="receiptUrl(id, file.id, true)" label="Descargar" icon="pi pi-download" text size="small" />
          </div>
        </li>
      </ul>
      <ListPaginator :page="page" :size="size" :total="total" @change="paginate" />
    </template></Card>
    <Card><template #title>Agregar comprobantes</template><template #content>
      <p class="mb-4">Si adjuntaste un archivo equivocado, agregá el correcto y dejá un comentario para aclararlo. Los archivos enviados se conservan y no pueden borrarse.</p>
      <form class="flex flex-col gap-4" :aria-busy="saving" @submit.prevent="submit">
        <Message v-if="error || validation" severity="error" role="alert">{{ validation || error }}</Message>
        <ReceiptPicker :model-value="receipts" :max-receipts="limits?.maxReceipts ?? 5" :max-file-bytes="limits?.maxFileBytes ?? 10485760" :disabled="saving" @update:model-value="select" />
        <div class="flex justify-end"><Button type="submit" label="Agregar comprobantes" :loading="saving" :disabled="!receipts.length" /></div>
      </form>
    </template></Card>
    <section aria-label="Comentarios públicos"><h2 class="mb-4 text-xl font-semibold">Comentarios públicos</h2><PortalCommentsPage :payment-id="id" /></section>
  </div>
</template>
