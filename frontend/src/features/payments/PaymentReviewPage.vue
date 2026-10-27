<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Dialog from "primevue/dialog";
import Message from "primevue/message";
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
import { reviewDetail, rejectPayment, receiptUrl, paymentLabels, originLabel, type ReviewDetail, type ReviewComment, type ReviewReceipt, type History, type LicenseEvent } from "./review";
const route = useRoute(), router = useRouter();
const id = Number(route.params.id), endpoint = `/web/api/payments/${id}`;
const back = computed(() => typeof route.query.returnTo === "string" && route.query.returnTo.startsWith("/payments") && router.resolve(route.query.returnTo).path === "/payments" ? route.query.returnTo : "/payments");
const confirm = useConfirmAction();
const detail = ref<ReviewDetail | null>(null), loading = ref(true), loadError = ref("");
const approving = ref(false), rejecting = ref(false);
const form = ref({ text: "", visibility: "PUBLICO" as "PUBLICO" | "PRIVADO" });
const rejection = ref({ reason: "" });
const { saving: rejectingSave, error: rejectionError, fields: rejectionFields, save: saveRejection, reset: resetRejection } = useDraft(rejection);
const { saving, error, fields, save, reset } = useDraft(form);
const comments = usePagedList<ReviewComment>(endpoint + "/comments");
const receipts = usePagedList<ReviewReceipt>(endpoint + "/attachments");
const history = usePagedList<History>(endpoint + "/history");
const licenseActions = usePagedList<LicenseEvent>(endpoint + "/license-actions");
const visibilities = [{ label: "Público: visible para el cliente", value: "PUBLICO" }, { label: "Privado: solo administración", value: "PRIVADO" }];
async function load() {
  loading.value = true; loadError.value = "";
  try { detail.value = await reviewDetail(id); }
  catch (failure) { loadError.value = failure instanceof Error ? failure.message : "No se pudo abrir el pago."; }
  finally { loading.value = false; }
}
onMounted(load);
async function postComment() {
  const value = await save(() => request<ReviewComment>(endpoint + "/comments", { method: "POST", body: JSON.stringify({ text: form.value.text.trim(), visibility: form.value.visibility }) }), "Se agregó el comentario.");
  if (value) { form.value.text = ""; reset(); await comments.load(); }
}
async function reject() {
  const current = detail.value;
  if (!current || !rejection.value.reason.trim()) return;
  const result = await saveRejection(() => rejectPayment(id, current.version, rejection.value.reason.trim()), "El pago quedó rechazado.");
  if (result) { detail.value = result; rejection.value.reason = ""; resetRejection(); rejecting.value = false; await history.load(); }
}
async function closeRejection() {
  if (rejectingSave.value) return;
  if (rejection.value.reason && !await confirm("¿Descartar el motivo de rechazo escrito?", "Cerrar rechazo")) return;
  rejection.value.reason = ""; resetRejection(); rejecting.value = false; rejectionError.value = "";
}
async function refreshAll() { await Promise.all([load(), comments.load(), receipts.load(), history.load(), licenseActions.load()]); }
async function approved() { approving.value = false; await refreshAll(); }
</script>
<template>
  <PageHeader :title="'Revisión del pago #' + id" description="Datos presentados, evidencias e historial de revisión.">
    <Button as="router-link" :to="back" label="Volver a pagos" icon="pi pi-arrow-left" text :disabled="saving" />
    <Button type="button" label="Actualizar detalle" icon="pi pi-refresh" outlined :disabled="saving || loading" @click="refreshAll" />
  </PageHeader>
  <Skeleton v-if="loading" height="12rem" />
  <Message v-else-if="loadError" severity="error" role="alert">{{ loadError }}</Message>
  <div v-else-if="detail" class="flex min-w-0 flex-col gap-6">
    <Card><template #title><div class="flex flex-wrap items-center justify-between gap-3"><span>{{ money(detail.payment.amount) }} · {{ detail.payment.currency }}</span><Tag :value="paymentLabels[detail.payment.status]" :severity="detail.payment.status === 'APROBADO' ? 'success' : detail.payment.status === 'RECHAZADO' ? 'danger' : 'warn'" /></div></template><template #content>
      <dl class="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <div><dt class="text-sm text-muted-color">Cliente</dt><dd><RouterLink :to="'/clients/' + detail.payment.clientId" class="font-medium text-primary">{{ detail.payment.clientName }}</RouterLink></dd></div>
        <div><dt class="text-sm text-muted-color">Licencia asociada</dt><dd>{{ detail.payment.companyName || 'Sin licencia asociada' }}</dd></div>
        <div><dt class="text-sm text-muted-color">Emisión</dt><dd>{{ displayDateTime(detail.payment.issuedAt) }}</dd></div>
        <div><dt class="text-sm text-muted-color">Presentación</dt><dd>{{ displayDateTime(detail.createdAt) }}</dd></div>
        <div><dt class="text-sm text-muted-color">Teléfono informado</dt><dd class="break-words">{{ detail.contactPhone }}</dd></div>
        <div><dt class="text-sm text-muted-color">Forma de pago</dt><dd>Transferencia</dd></div>
        <div><dt class="text-sm text-muted-color">Banco destinatario original</dt><dd class="break-words">{{ detail.destinationBank }}</dd></div>
        <div><dt class="text-sm text-muted-color">Titular original</dt><dd class="break-words">{{ detail.destinationHolder }}</dd></div>
        <div><dt class="text-sm text-muted-color">CBU original</dt><dd class="break-all font-mono">{{ detail.destinationCbu }}</dd></div>
        <div><dt class="text-sm text-muted-color">Alias original</dt><dd class="break-all">{{ detail.destinationAlias || 'Sin alias' }}</dd></div>
        <div><dt class="text-sm text-muted-color">Usuario atribuido</dt><dd>{{ detail.submittedBy?.name || 'Sin dato histórico' }}<span v-if="detail.submittedBy"> (#{{ detail.submittedBy.id }})</span></dd></div>
        <div><dt class="text-sm text-muted-color">Origen de la presentación</dt><dd>{{ originLabel(detail.origin) }}</dd></div>
        <div v-if="detail.withoutReceipt" class="sm:col-span-2"><dt class="text-sm text-muted-color">Motivo original de ausencia de comprobante</dt><dd class="whitespace-pre-wrap break-words">{{ detail.noReceiptReason }}</dd></div>
      </dl>
      <div v-if="detail.payment.status === 'PENDIENTE'" class="mt-6 flex flex-wrap justify-end gap-3">
        <Button type="button" label="Rechazar" icon="pi pi-times" severity="danger" outlined :disabled="saving" @click="rejecting = true; rejectionError = ''" />
        <Button type="button" label="Aprobar" icon="pi pi-check" :disabled="saving" @click="approving = true" />
      </div>
      <Message v-else severity="info" :closable="false" class="mt-5">Este pago ya fue resuelto y no puede reabrirse.</Message>
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
    <Card><template #title>Comentarios</template><template #content>
      <Message v-if="comments.error.value" severity="error">{{ comments.error.value }}</Message><Skeleton v-if="comments.loading.value" height="6rem" />
      <p v-else-if="!comments.rows.value.length" class="text-muted-color">Todavía no hay comentarios.</p>
      <ul class="flex flex-col gap-4"><li v-for="comment in comments.rows.value" :key="comment.id" class="border-b border-surface pb-3">
        <div class="mb-2 flex flex-wrap items-center gap-2"><Tag :value="comment.visibility === 'PRIVADO' ? 'Privado' : 'Público'" :severity="comment.visibility === 'PRIVADO' ? 'secondary' : 'info'" /><span class="text-sm text-muted-color">{{ displayDateTime(comment.occurredAt) }} · {{ comment.author?.name || 'Sin dato histórico' }} · {{ originLabel(comment.origin) }}</span></div>
        <p class="whitespace-pre-wrap break-words">{{ comment.text }}</p>
      </li></ul><ListPaginator :page="comments.page.value" :size="comments.size.value" :total="comments.total.value" @change="comments.paginate" />
      <form class="mt-6 flex flex-col gap-4" :aria-busy="saving" @submit.prevent="postComment">
        <Message v-if="error && !rejecting" severity="error" role="alert">{{ error }}</Message>
        <FormField id="reviewVisibility" label="Visibilidad" required><Select input-id="reviewVisibility" v-model="form.visibility" :options="visibilities" option-label="label" option-value="value" :disabled="saving" fluid /></FormField>
        <FormField id="reviewComment" label="Nuevo comentario" required :error="fields.text" v-slot="field"><Textarea id="reviewComment" v-model="form.text" maxlength="4000" rows="3" required :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid /></FormField>
        <p class="text-sm text-muted-color">El comentario no podrá editarse ni borrarse después de enviarlo.</p>
        <div class="flex justify-end"><Button type="submit" label="Agregar comentario" :loading="saving" :disabled="!form.text.trim()" /></div>
      </form>
    </template></Card>
    <Card><template #title>Historial de estados</template><template #content>
      <Message v-if="history.error.value" severity="error">{{ history.error.value }}</Message><Skeleton v-if="history.loading.value" height="5rem" />
      <ol class="flex flex-col gap-4"><li v-for="event in history.rows.value" :key="event.id" class="border-b border-surface pb-3"><p class="font-medium">{{ event.previousStatus || 'Creación' }} → {{ event.newStatus }}</p><p class="text-sm text-muted-color">{{ displayDateTime(event.occurredAt) }} · {{ event.responsible?.name || 'Sin dato histórico' }} · {{ originLabel(event.origin) }}</p><p v-if="event.reason" class="whitespace-pre-wrap break-words">{{ event.reason }}</p></li></ol>
      <ListPaginator :page="history.page.value" :size="history.size.value" :total="history.total.value" @change="history.paginate" />
    </template></Card>
    <Card><template #title>Acciones sobre licencias</template><template #content>
      <Message v-if="licenseActions.error.value" severity="error">{{ licenseActions.error.value }}</Message><Skeleton v-if="licenseActions.loading.value" height="5rem" />
      <p v-else-if="!licenseActions.rows.value.length" class="text-muted-color">No se registraron cambios de licencia asociados a este pago.</p>
      <ul class="flex flex-col gap-4"><li v-for="event in licenseActions.rows.value" :key="event.id" class="border-b border-surface pb-3"><p class="font-medium">{{ event.companyName }} (#{{ event.licenseId }}) · {{ event.actionType === 'DESBLOQUEO' ? 'Bloqueo forzado' : 'Vencimiento' }}</p><p>{{ event.previousValue ?? 'Sin valor anterior' }} → {{ event.newValue }}</p><p class="text-sm text-muted-color">{{ displayDateTime(event.occurredAt) }} · {{ event.responsible?.name || 'Sin dato histórico' }}</p></li></ul>
      <ListPaginator :page="licenseActions.page.value" :size="licenseActions.size.value" :total="licenseActions.total.value" @change="licenseActions.paginate" />
    </template></Card>
  </div>
  <PaymentApprovalDialog v-if="approving && detail" :detail="detail" @close="approving = false" @approved="approved" />
  <Dialog :visible="rejecting" modal header="Rechazar pago" class="w-[min(36rem,calc(100vw-2rem))]" :closable="!rejectingSave" :close-on-escape="!rejectingSave" @update:visible="!$event && closeRejection()">
    <form id="rejectPayment" class="flex flex-col gap-4" :aria-busy="rejectingSave" @submit.prevent="reject">
      <Message severity="warn" :closable="false">El motivo será público y visible para el cliente. El rechazo no puede revertirse.</Message>
      <Message v-if="rejectionError" severity="error" role="alert">{{ rejectionError }}</Message>
      <FormField id="rejectionReason" label="Motivo público del rechazo" required :error="rejectionFields.reason" v-slot="field"><Textarea id="rejectionReason" v-model="rejection.reason" maxlength="1000" rows="4" required :disabled="rejectingSave" :invalid="field.invalid" :aria-describedby="field.describedby" fluid /></FormField>
    </form>
    <template #footer><Button type="button" label="Cancelar" text severity="secondary" :disabled="rejectingSave" @click="closeRejection" /><Button type="submit" form="rejectPayment" label="Confirmar rechazo" severity="danger" :loading="rejectingSave" :disabled="!rejection.reason.trim()" /></template>
  </Dialog>
</template>
