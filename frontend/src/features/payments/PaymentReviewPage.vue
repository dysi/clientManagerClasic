<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Dialog from "primevue/dialog";
import Message from "primevue/message";
import PaymentSyncWarning from "./PaymentSyncWarning.vue";
import Select from "primevue/select";
import Skeleton from "primevue/skeleton";
import Tag from "primevue/tag";
import Textarea from "primevue/textarea";
import PageHeader from "../../components/PageHeader.vue";
import FormField from "../../components/FormField.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import { useDraft } from "../../composables/useDraft";
import { usePagedList } from "../../composables/usePagedList";
import { useConfirmAction } from "../../composables/useConfirmAction";
import { request } from "../../services/http";
import { displayDateTime, money } from "../../services/format";
import PaymentApprovalDialog from "./PaymentApprovalDialog.vue";
import PaymentCorrectionDialog from "./PaymentCorrectionDialog.vue";
import { reviewDetail, rejectPayment, receiptUrl, paymentLabels, originLabel, type ReviewDetail, type ReviewComment, type ReviewReceipt, type ReviewActivity, type LicenseEvent } from "./review";
const route = useRoute(), router = useRouter();
const id = Number(route.params.id), endpoint = `/web/api/payments/${id}`;
const back = computed(() => typeof route.query.returnTo === "string" && route.query.returnTo.startsWith("/payments") && router.resolve(route.query.returnTo).path === "/payments" ? route.query.returnTo : "/payments");
const confirm = useConfirmAction();
const detail = ref<ReviewDetail | null>(null), loading = ref(true), loadError = ref("");
const approving = ref(false), rejecting = ref(false);
const editing = ref(false);
async function corrected(value: ReviewDetail) {
  detail.value = value; editing.value = false; await activity.search();
}
const form = ref({ text: "", visibility: "PUBLICO" as "PUBLICO" | "PRIVADO" });
const rejection = ref({ reason: "" });
const { saving: rejectingSave, error: rejectionError, fields: rejectionFields, save: saveRejection, reset: resetRejection } = useDraft(rejection);
const { saving, error, fields, save, reset } = useDraft(form);
const activity = usePagedList<ReviewActivity>(endpoint + "/activity");
const receipts = usePagedList<ReviewReceipt>(endpoint + "/attachments");
const licenseActions = usePagedList<LicenseEvent>(endpoint + "/license-actions");
const visibilities = [{ label: "Público", value: "PUBLICO" }, { label: "Privado", value: "PRIVADO" }];
async function load() {
  loading.value = true; loadError.value = "";
  try { detail.value = await reviewDetail(id); }
  catch (failure) { loadError.value = failure instanceof Error ? failure.message : "No se pudo abrir el pago."; }
  finally { loading.value = false; }
}
onMounted(load);
async function postComment() {
  const value = await save(() => request<ReviewComment>(endpoint + "/comments", { method: "POST", body: JSON.stringify({ text: form.value.text.trim(), visibility: form.value.visibility }) }), "Se agregó el comentario.");
  if (value) { form.value.text = ""; reset(); await activity.search(); }
}
async function reject() {
  const current = detail.value;
  if (!current || !rejection.value.reason.trim()) return;
  const result = await saveRejection(() => rejectPayment(id, current.version, rejection.value.reason.trim()), "El pago quedó rechazado.");
  if (result) { detail.value = result; rejection.value.reason = ""; resetRejection(); rejecting.value = false; await activity.search(); }
}
async function closeRejection() {
  if (rejectingSave.value) return;
  if (rejection.value.reason && !await confirm("¿Descartar el motivo de rechazo escrito?", "Cerrar rechazo")) return;
  rejection.value.reason = ""; resetRejection(); rejecting.value = false; rejectionError.value = "";
}
async function refreshAll() { await Promise.all([load(), activity.search(), receipts.load(), licenseActions.load()]); }
async function approved() { approving.value = false; await refreshAll(); }
</script>
<template>
  <PageHeader :title="'Revisión del pago #' + id" description="Datos presentados, evidencias e historial de revisión.">
    <Button as="router-link" :to="back" label="Volver a pagos" icon="pi pi-arrow-left" text :disabled="saving" />
    <Button type="button" label="Actualizar detalle" icon="pi pi-refresh" outlined :disabled="saving || loading" @click="refreshAll" />
  </PageHeader>
  <Skeleton v-if="loading" height="12rem" />
  <Message v-else-if="loadError" severity="error" role="alert">{{ loadError }}</Message>
  <div v-else-if="detail" class="grid min-w-0 items-start gap-6 lg:grid-cols-[minmax(0,4fr)_minmax(16rem,1fr)]">
    <div class="flex min-w-0 flex-col gap-6">
    <Card><template #title><div class="flex flex-wrap items-center justify-between gap-3"><span>{{ money(detail.payment.amount) }} · {{ detail.payment.currency }}</span><Tag :value="paymentLabels[detail.payment.status]" :severity="detail.payment.status === 'APROBADO' ? 'success' : detail.payment.status === 'RECHAZADO' ? 'danger' : 'warn'" /></div></template><template #content>
      <PaymentSyncWarning :synchronized-at="detail.payment.angryboxSynchronizedAt" />
      <dl class="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <div><dt class="text-sm text-muted-color">Cliente</dt><dd><RouterLink :to="'/clients/' + detail.payment.clientId" class="font-medium text-primary">{{ detail.payment.clientName }}</RouterLink></dd></div>
        <div><dt class="text-sm text-muted-color">Licencia asociada</dt><dd>{{ detail.payment.companyName || 'Sin licencia asociada' }}</dd></div>
        <div><dt class="text-sm text-muted-color">Emisión</dt><dd>{{ displayDateTime(detail.payment.issuedAt) }}</dd></div>
        <div><dt class="text-sm text-muted-color">Presentación</dt><dd>{{ displayDateTime(detail.createdAt) }}</dd></div>
        <div><dt class="text-sm text-muted-color">Teléfono informado</dt><dd class="break-words">{{ detail.contactPhone }}</dd></div>
        <div><dt class="text-sm text-muted-color">Forma de pago</dt><dd>Transferencia</dd></div>
        <div><dt class="text-sm text-muted-color">Banco destinatario</dt><dd class="break-words">{{ detail.destinationBank }}</dd></div>
        <div><dt class="text-sm text-muted-color">Titular destinatario</dt><dd class="break-words">{{ detail.destinationHolder }}</dd></div>
        <div><dt class="text-sm text-muted-color">CBU destinatario</dt><dd class="break-all font-mono">{{ detail.destinationCbu }}</dd></div>
        <div><dt class="text-sm text-muted-color">Alias destinatario</dt><dd class="break-all">{{ detail.destinationAlias || 'Sin alias' }}</dd></div>
        <div><dt class="text-sm text-muted-color">Usuario atribuido</dt><dd>{{ detail.submittedBy?.name || 'Sin dato histórico' }}<span v-if="detail.submittedBy"> (#{{ detail.submittedBy.id }})</span></dd></div>
        <div><dt class="text-sm text-muted-color">Origen de la presentación</dt><dd>{{ originLabel(detail.origin) }}</dd></div>
        <div v-if="detail.withoutReceipt" class="sm:col-span-2"><dt class="text-sm text-muted-color">Motivo original de ausencia de comprobante</dt><dd class="whitespace-pre-wrap break-words">{{ detail.noReceiptReason }}</dd></div>
      </dl>
      <div class="mt-6 flex flex-wrap justify-end gap-3">
        <Button type="button" label="Editar pago" icon="pi pi-pencil" outlined :disabled="saving" @click="editing = true" />
        <Button v-if="detail.payment.status !== 'RECHAZADO'" type="button" label="Rechazar" icon="pi pi-times" severity="danger" outlined :disabled="saving" @click="rejecting = true; rejectionError = ''" />
        <Button v-if="detail.payment.status !== 'APROBADO'" type="button" label="Aprobar" icon="pi pi-check" :disabled="saving" @click="approving = true" />
      </div>
    </template></Card>
    <Card><template #title>Comprobantes</template><template #content>
      <Message v-if="receipts.error.value" severity="error">{{ receipts.error.value }}</Message><Skeleton v-if="receipts.loading.value" height="6rem" />
      <p v-else-if="!receipts.rows.value.length" class="text-muted-color">No hay comprobantes adjuntos.</p>
      <ul class="flex flex-col gap-4"><li v-for="file in receipts.rows.value" :key="file.id" class="min-w-0 border-b border-surface pb-3">
        <p class="break-all font-medium">{{ file.originalName }}</p><p v-if="file.description" class="whitespace-pre-wrap break-words">{{ file.description }}</p>
        <p class="text-sm text-muted-color">{{ displayDateTime(file.uploadedAt) }} · {{ file.uploadedBy?.name || 'Sin dato histórico' }} · {{ originLabel(file.origin) }} · {{ (file.size / 1048576).toFixed(2) }} MB</p>
        <div class="mt-2 flex flex-wrap gap-2"><Button as="a" :href="receiptUrl(id, file.id)" target="_blank" rel="noopener noreferrer" label="Abrir comprobante" text size="small" /><Button as="a" :href="receiptUrl(id, file.id, true)" label="Descargar" text size="small" /></div>
      </li></ul><ListPaginator :page="receipts.page.value" :size="receipts.size.value" :total="receipts.total.value" @change="receipts.paginate" />
    </template></Card>
    </div>
    <aside class="flex min-w-0 flex-col gap-6" aria-label="Actividad de revisión del pago">
    <Card :pt="{ body: { class: 'min-w-0 p-4' } }"><template #title>Actividad del pago</template><template #content>
      <p class="mb-4 text-sm text-muted-color">Comentarios y estados · más recientes primero.</p>
      <details class="mb-5 rounded-border border border-surface p-3">
      <summary class="cursor-pointer font-medium text-primary">Agregar comentario</summary>
      <form class="mt-4 flex min-w-0 flex-col gap-4" :aria-busy="saving" @submit.prevent="postComment">
        <Message v-if="error && !rejecting" severity="error" role="alert">{{ error }}</Message>
        <FormField id="reviewVisibility" label="Visibilidad" required :hint="form.visibility === 'PUBLICO' ? 'Visible para el cliente.' : 'Solo visible para administración.'" v-slot="field"><Select input-id="reviewVisibility" v-model="form.visibility" :options="visibilities" option-label="label" option-value="value" :disabled="saving" :aria-describedby="field.describedby" fluid /></FormField>
        <FormField id="reviewComment" label="Nuevo comentario" required :error="fields.text" v-slot="field"><Textarea id="reviewComment" v-model="form.text" maxlength="4000" rows="3" required :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid /></FormField>
        <p class="text-sm text-muted-color">El comentario no podrá editarse ni borrarse después de enviarlo.</p>
        <div class="flex justify-end"><Button type="submit" label="Agregar comentario" :loading="saving" :disabled="!form.text.trim()" /></div>
      </form>
      </details>
      <Message v-if="activity.error.value" severity="error" role="alert">{{ activity.error.value }} <Button type="button" label="Reintentar" text size="small" @click="activity.load" /></Message>
      <Skeleton v-if="activity.loading.value" height="10rem" />
      <p v-else-if="!activity.error.value && !activity.rows.value.length" class="text-muted-color">Todavía no hay actividad registrada.</p>
      <div v-else-if="activity.rows.value.length" class="max-h-[22rem] overflow-y-auto overscroll-contain pr-2" tabindex="0" role="region" aria-label="Línea de tiempo del pago" :aria-busy="activity.loading.value">
        <ol class="ml-2 border-l border-surface">
          <li v-for="event in activity.rows.value" :key="event.kind + '-' + (event.comment?.id ?? event.history?.id)" class="relative min-w-0 pb-6 pl-5 text-sm last:pb-1">
            <span class="absolute -left-2 top-0 flex size-4 items-center justify-center rounded-full bg-emphasis text-primary" aria-hidden="true"><i :class="event.kind === 'COMENTARIO' ? 'pi pi-comment' : 'pi pi-history'" class="text-[0.6rem]" /></span>
            <template v-if="event.kind === 'COMENTARIO'">
              <Tag :value="event.comment.visibility === 'PRIVADO' ? 'Comentario privado' : 'Comentario público'" :severity="event.comment.visibility === 'PRIVADO' ? 'secondary' : 'info'" />
              <p class="mt-2 whitespace-pre-wrap break-words">{{ event.comment.text }}</p>
              <p class="mt-2 break-words text-xs text-muted-color">{{ event.comment.author?.name || 'Sin dato histórico' }} · {{ originLabel(event.comment.origin) }}</p>
              <time class="text-xs text-muted-color" :datetime="event.comment.occurredAt">{{ displayDateTime(event.comment.occurredAt) }}</time>
            </template>
            <template v-else>
              <p class="font-medium">{{ event.history.previousStatus ? paymentLabels[event.history.previousStatus] : 'Creación' }} → {{ paymentLabels[event.history.newStatus] }}</p>
              <p v-if="event.history.reason" class="mt-2 whitespace-pre-wrap break-words">{{ event.history.reason }}</p>
              <p class="mt-2 break-words text-xs text-muted-color">{{ event.history.responsible?.name || 'Sin dato histórico' }} · {{ originLabel(event.history.origin) }}</p>
              <time class="text-xs text-muted-color" :datetime="event.history.occurredAt">{{ displayDateTime(event.history.occurredAt) }}</time>
            </template>
          </li>
        </ol>
      </div>
      <ListPaginator v-if="activity.total.value" :page="activity.page.value" :size="activity.size.value" :total="activity.total.value" @change="activity.paginate" />
    </template></Card>
    <Card :pt="{ body: { class: 'min-w-0 p-4' } }"><template #title>Acciones sobre licencias</template><template #content>
      <Message v-if="licenseActions.error.value" severity="error">{{ licenseActions.error.value }}</Message><Skeleton v-if="licenseActions.loading.value" height="5rem" />
      <p v-else-if="!licenseActions.rows.value.length" class="text-muted-color">No se registraron cambios de licencia asociados a este pago.</p>
      <ul class="flex max-h-64 flex-col gap-4 overflow-y-auto text-sm"><li v-for="event in licenseActions.rows.value" :key="event.id" class="min-w-0 border-b border-surface pb-3"><p class="break-words font-medium">{{ event.companyName }} (#{{ event.licenseId }}) · {{ event.actionType === 'DESBLOQUEO' ? 'Bloqueo forzado' : 'Vencimiento' }}</p><p class="break-words">{{ event.previousValue ?? 'Sin valor anterior' }} → {{ event.newValue }}</p><p class="text-xs text-muted-color">{{ displayDateTime(event.occurredAt) }} · {{ event.responsible?.name || 'Sin dato histórico' }}</p></li></ul>
      <ListPaginator v-if="licenseActions.total.value" :page="licenseActions.page.value" :size="licenseActions.size.value" :total="licenseActions.total.value" @change="licenseActions.paginate" />
    </template></Card>
    </aside>
  </div>
  <PaymentApprovalDialog v-if="approving && detail" :detail="detail" @close="approving = false" @approved="approved" />
  <PaymentCorrectionDialog v-if="editing && detail" :detail="detail" @close="editing = false" @saved="corrected" />
  <Dialog :visible="rejecting" modal header="Rechazar pago" class="w-[min(36rem,calc(100vw-2rem))]" :closable="!rejectingSave" :close-on-escape="!rejectingSave" @update:visible="!$event && closeRejection()">
    <form id="rejectPayment" class="flex flex-col gap-4" :aria-busy="rejectingSave" @submit.prevent="reject">
      <Message severity="warn" :closable="false">El motivo será público y visible para el cliente. El cambio queda registrado con tu usuario. No revierte acciones anteriores sobre licencias.</Message>
      <PaymentSyncWarning :synchronized-at="detail?.payment.angryboxSynchronizedAt" />
      <Message v-if="rejectionError" severity="error" role="alert">{{ rejectionError }}</Message>
      <FormField id="rejectionReason" label="Motivo público del rechazo" required :error="rejectionFields.reason" v-slot="field"><Textarea id="rejectionReason" v-model="rejection.reason" maxlength="1000" rows="4" required :disabled="rejectingSave" :invalid="field.invalid" :aria-describedby="field.describedby" fluid /></FormField>
    </form>
    <template #footer><Button type="button" label="Cancelar" text severity="secondary" :disabled="rejectingSave" @click="closeRejection" /><Button type="submit" form="rejectPayment" label="Confirmar rechazo" severity="danger" :loading="rejectingSave" :disabled="!rejection.reason.trim()" /></template>
  </Dialog>
</template>
