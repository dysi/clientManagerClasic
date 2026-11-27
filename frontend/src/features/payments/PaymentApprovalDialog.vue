<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import Button from "primevue/button";
import Checkbox from "primevue/checkbox";
import Dialog from "primevue/dialog";
import Message from "primevue/message";
import PaymentSyncWarning from "./PaymentSyncWarning.vue";
import Skeleton from "primevue/skeleton";
import Tag from "primevue/tag";
import Textarea from "primevue/textarea";
import DateInput from "../../components/DateInput.vue";
import FormField from "../../components/FormField.vue";
import LicenseStatus from "../../components/LicenseStatus.vue";
import { useConfirmAction } from "../../composables/useConfirmAction";
import { useDraft } from "../../composables/useDraft";
import { displayDate, toDate } from "../../services/format";
import { approvalLicenses, approvePayment, previewApproval, type ReviewDetail, type LicenseOption, type LicenseAction, type ApprovalPreview } from "./review";
const props = defineProps<{ detail: ReviewDetail }>();
const emit = defineEmits<{ close: []; approved: [] }>();
const confirm = useConfirmAction();
interface Selection { license: LicenseOption; editable: boolean; unlock: boolean; updateExpiry: boolean; lockDate: string | null; confirmShortening: boolean }
const draft = ref<{ reason: string; updateAll: boolean; commonDate: string | null; selections: Selection[] }>({ reason: "", updateAll: true, commonDate: null, selections: [] });
const { saving, error, save, dirty, reset } = useDraft(draft);
const loading = ref(false), loadError = ref(""), loaded = ref(false), today = ref("");
const step = ref<"adjust" | "review">("adjust");
const preview = ref<ApprovalPreview | null>(null), previewing = ref(false);
let active = true, sequence = 0;
onUnmounted(() => { active = false; sequence++; });
async function load() {
  if (loading.value) return;
  loading.value = true; loadError.value = "";
  try {
    const result = await approvalLicenses(props.detail.payment.id);
    if (!active) return;
    today.value = result.today; draft.value.commonDate = result.suggestedDate;
    draft.value.selections = result.licenses.map(license => ({ license, editable: true, unlock: false,
      updateExpiry: false, lockDate: license.suggestedDate, confirmShortening: false }));
    loaded.value = true; reset();
  } catch (failure) { if (active) loadError.value = failure instanceof Error ? failure.message : "No se pudieron cargar las licencias."; }
  finally { if (active) loading.value = false; }
}
onMounted(load);
function commonEligible(license: LicenseOption) { return !license.hidden && !license.forcedLock; }
/** La actualización general conserva vencimientos posteriores y nunca quita un bloqueo forzado. */
function action(row: Selection): LicenseAction {
  let unlock = false, updateExpiry = false, lockDate: string | null = null;
  if (draft.value.updateAll) {
    if (commonEligible(row.license)) {
      const date = draft.value.commonDate;
      lockDate = date && row.license.lockDate && row.license.lockDate > date ? row.license.lockDate : date;
      updateExpiry = lockDate !== row.license.lockDate;
    }
  } else if (row.editable) {
    unlock = row.unlock && row.license.forcedLock;
    updateExpiry = row.updateExpiry && row.lockDate !== row.license.lockDate;
    lockDate = row.lockDate;
  }
  return { licenseId: row.license.id, revision: row.license.revision, unlock, updateExpiry,
    lockDate: updateExpiry ? lockDate : null, confirmShortening: row.confirmShortening };
}
const actions = computed(() => draft.value.selections.map(action));
const changedCount = computed(() => actions.value.filter(item => item.unlock || item.updateExpiry).length);
const summary = computed(() => `${draft.value.selections.length} licencias: ${changedCount.value} se actualizarán, ${draft.value.selections.length - changedCount.value} quedarán sin cambios.`);
const commonDateError = computed(() => !draft.value.commonDate ? "Elegí el vencimiento propuesto."
  : draft.value.commonDate < today.value ? "El vencimiento no puede ser anterior a hoy." : "");
function individualDateError(row: Selection) {
  return !draft.value.updateAll && row.editable && row.updateExpiry
    ? !row.lockDate ? "Elegí el vencimiento propuesto." : row.lockDate < today.value ? "El vencimiento no puede ser anterior a hoy." : ""
    : "";
}
const canReview = computed(() => loaded.value && !loading.value && !previewing.value && !saving.value
  && (!draft.value.updateAll || !commonDateError.value) && !draft.value.selections.some(row => individualDateError(row)));
function calendarDay(value: string) {
  const date = toDate(value);
  return date ? Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / 86400000 : 0;
}
function dateDescription(value: string | null) {
  const date = toDate(value);
  if (!date || !today.value) return "Sin fecha propuesta";
  const days = calendarDay(value ?? "") - calendarDay(today.value);
  const label = new Intl.DateTimeFormat("es-AR", { day: "numeric", month: "long", year: "numeric" }).format(date);
  return `Vence el ${label} (${days} ${days === 1 ? 'día' : 'días'})`;
}
function proposedDate(row: Selection) { const item = action(row); return item.updateExpiry ? item.lockDate : row.license.lockDate; }
const signature = computed(() => JSON.stringify({ all: draft.value.updateAll, date: draft.value.commonDate,
  rows: draft.value.selections.map(row => [row.license.id, row.license.revision, row.editable, row.unlock, row.updateExpiry, row.lockDate]) }));
watch(signature, () => {
  sequence++; preview.value = null; step.value = "adjust";
  for (const row of draft.value.selections) row.confirmShortening = false;
}, { flush: "sync" });
async function review() {
  if (!canReview.value) return;
  const current = ++sequence; previewing.value = true; error.value = "";
  try {
    const result = await previewApproval(props.detail.payment.id, props.detail.version, actions.value);
    if (active && current === sequence) { preview.value = result; step.value = "review"; }
  } catch (failure) { if (active && current === sequence) error.value = failure instanceof Error ? failure.message : "No se pudo preparar la revisión."; }
  finally { if (active) previewing.value = false; }
}
const ready = computed(() => step.value === "review" && preview.value != null && preview.value.licenses.every(item =>
  !item.shortening || draft.value.selections.find(row => row.license.id === item.id)?.confirmShortening));
function confirmShortening(id: number, value: boolean) {
  const row = draft.value.selections.find(item => item.license.id === id);
  if (row) row.confirmShortening = value;
}
async function approve() {
  const reviewed = preview.value;
  if (!reviewed || !ready.value || saving.value) return;
  const result = await save(() => approvePayment(props.detail.payment.id, props.detail.version, actions.value, reviewed, draft.value.reason.trim()), "El pago quedó aprobado.");
  if (result) { reset(); emit("approved"); }
  else { preview.value = null; step.value = "adjust"; }
}
function back() { if (!saving.value) { step.value = "adjust"; error.value = ""; } }
async function close() {
  if (!saving.value && (!dirty.value || await confirm("¿Descartar los ajustes de esta aprobación?", "Cerrar aprobación"))) {
    reset(); emit("close");
  }
}
</script>
<template>
  <Dialog :visible="true" modal :header="step === 'adjust' ? 'Aprobar pago · 1 de 2: ajustar licencias' : 'Aprobar pago · 2 de 2: revisar y confirmar'" class="w-[min(64rem,calc(100vw-2rem))]" :closable="!saving" :close-on-escape="!saving" @update:visible="!$event && close()">
    <div class="flex min-w-0 flex-col gap-4 text-sm" :aria-busy="saving || previewing || loading">
      <Message v-if="error" severity="error" role="alert">{{ error }}</Message>
      <Message v-if="loadError" severity="error" role="alert">{{ loadError }} <Button type="button" label="Reintentar" text size="small" @click="load" /></Message>
      <Skeleton v-if="loading" height="12rem" />
      <template v-else-if="loaded">
        <FormField id="approvalReason" label="Comentario público del auditor (opcional)" v-slot="field"><Textarea id="approvalReason" v-model="draft.reason" rows="3" maxlength="1000" :disabled="saving" :aria-describedby="field.describedby" fluid /></FormField>
        <Message severity="info" :closable="false">La decisión queda registrada con tu usuario. Si agregás un comentario, será visible para el cliente. No revierte acciones anteriores sobre licencias.</Message>
        <PaymentSyncWarning :synchronized-at="detail.payment.angryboxSynchronizedAt" />
        <p class="font-medium" role="status">{{ summary }}</p>
        <template v-if="step === 'adjust'">
          <section class="rounded-border border border-surface p-4" aria-label="Actualización general">
            <div class="flex items-center gap-2"><Checkbox input-id="updateAllExpiries" v-model="draft.updateAll" binary :disabled="saving || previewing" /><label for="updateAllExpiries" class="font-semibold">Actualizar vencimiento a todas las licencias</label></div>
            <p class="mt-2 text-muted-color">Se aplica a las visibles sin bloqueo forzado, incluidas las vencidas. Se conservan los vencimientos posteriores a la fecha propuesta.</p>
            <div v-if="draft.updateAll" class="mt-3 grid items-end gap-3 sm:grid-cols-2">
              <FormField id="commonExpiry" label="Vencimiento propuesto" required :error="commonDateError" v-slot="field"><DateInput input-id="commonExpiry" v-model="draft.commonDate" required :disabled="saving || previewing" :invalid="field.invalid" :describedby="field.describedby" /></FormField>
              <p class="pb-2" aria-live="polite">{{ dateDescription(draft.commonDate) }}</p>
            </div>
            <p v-else class="mt-2 text-muted-color">Ajustá cada licencia o marcala para mantenerla sin cambios.</p>
          </section>
          <p v-if="!draft.selections.length" class="text-muted-color">No hay licencias para ajustar. Podés aprobar el pago sin modificar licencias.</p>
          <article v-for="row in draft.selections" :key="row.license.id" class="min-w-0 rounded-border border border-surface p-3" :aria-label="row.license.companyName">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <h3 class="min-w-0 break-words text-sm font-semibold">{{ row.license.companyName }} (#{{ row.license.id }})</h3>
              <div class="flex flex-wrap gap-2"><LicenseStatus :status="row.license.status" /><Tag v-if="row.license.hidden" value="Oculta" severity="secondary" /><Tag v-if="!action(row).unlock && !action(row).updateExpiry" value="Sin cambios" severity="secondary" /></div>
            </div>
            <p class="mt-2">Vencimiento actual: <strong>{{ displayDate(row.license.lockDate) }}</strong></p>
            <template v-if="draft.updateAll">
              <p v-if="commonEligible(row.license)" class="mt-1">Vencimiento al aprobar: <strong>{{ displayDate(proposedDate(row)) }}</strong></p>
              <p v-else class="mt-1 text-muted-color">No se incluye en la actualización general. Desmarcá la opción general para ajustar esta licencia individualmente.</p>
            </template>
            <template v-else>
              <div v-if="row.editable" class="mt-3 grid gap-3 sm:grid-cols-2">
                <div v-if="row.license.forcedLock" class="flex items-center gap-2"><Checkbox :input-id="'unlock-' + row.license.id" v-model="row.unlock" binary :disabled="saving || previewing" /><label :for="'unlock-' + row.license.id">Quitar bloqueo forzado</label></div>
                <div class="flex items-center gap-2"><Checkbox :input-id="'expiry-' + row.license.id" v-model="row.updateExpiry" binary :disabled="saving || previewing" /><label :for="'expiry-' + row.license.id">Actualizar vencimiento</label></div>
                <FormField v-if="row.updateExpiry" :id="'date-' + row.license.id" label="Vencimiento propuesto" required :error="individualDateError(row)" v-slot="field"><DateInput :input-id="'date-' + row.license.id" v-model="row.lockDate" required :disabled="saving || previewing" :invalid="field.invalid" :describedby="field.describedby" /><p class="mt-1 text-muted-color">{{ dateDescription(row.lockDate) }}</p></FormField>
              </div>
              <p v-else class="mt-2 text-muted-color">Esta licencia se mantendrá sin cambios.</p>
              <Button type="button" :label="row.editable ? 'Mantener licencia sin cambios' : 'Ajustar licencia'" :icon="row.editable ? 'pi pi-lock' : 'pi pi-pencil'" text severity="secondary" size="small" class="mt-2" :disabled="saving || previewing" @click="row.editable = !row.editable" />
            </template>
          </article>
        </template>
        <section v-else-if="preview" class="flex min-w-0 flex-col gap-3" aria-label="Vista previa de aprobación">
          <p>Revisá el estado y vencimiento de todas las licencias antes de confirmar. Fecha de revisión: {{ displayDate(preview.today) }}.</p>
          <Message v-if="!changedCount" severity="info" :closable="false">Se aprobará el pago sin modificar licencias.</Message>
          <article v-for="item in preview.licenses" :key="item.id" class="min-w-0 rounded-border border border-surface p-3">
            <div class="flex flex-wrap items-center justify-between gap-2"><h3 class="min-w-0 break-words text-sm font-semibold">{{ item.companyName }} (#{{ item.id }})</h3><Tag v-if="!item.unlock && !item.updateExpiry" value="Sin cambios" severity="secondary" /></div>
            <div class="mt-2 flex flex-wrap items-center gap-2"><span>Estado:</span><LicenseStatus :status="item.status" /><span aria-label="pasa a">→</span><LicenseStatus :status="item.resultingStatus" /></div>
            <p class="mt-2">Vencimiento: <strong>{{ displayDate(item.lockDate) }}</strong> → <strong>{{ displayDate(item.proposedDate) }}</strong></p>
            <p v-if="item.proposedDate" class="mt-1 text-muted-color">{{ dateDescription(item.proposedDate) }}</p>
            <p v-if="item.unlock" class="mt-2">Se quitará el bloqueo forzado.</p>
            <Message v-if="(item.unlock || item.updateExpiry) && ['EXPIRED', 'BLOCKED', 'UNKNOWN'].includes(item.resultingStatus)" severity="warn" :closable="false" class="mt-2">Los cambios no dejan esta licencia vigente. Revisá su bloqueo y vencimiento.</Message>
            <template v-if="item.shortening"><Message severity="warn" :closable="false" class="mt-2">Se acortará el vencimiento de esta licencia.</Message><div class="mt-2 flex items-center gap-2"><Checkbox :input-id="'shorten-' + item.id" :model-value="draft.selections.find(row => row.license.id === item.id)?.confirmShortening ?? false" binary :disabled="saving" @update:model-value="confirmShortening(item.id, $event === true)" /><label :for="'shorten-' + item.id">Confirmo que quiero acortar este vencimiento</label></div></template>
          </article>
        </section>
        <p class="text-muted-color">La aprobación del pago y los ajustes se guardan juntos. El saldo informado del cliente no cambia.</p>
      </template>
    </div>
    <template #footer>
      <div class="flex w-full flex-wrap justify-end gap-2">
      <Button type="button" label="Cancelar" severity="secondary" text :disabled="saving" @click="close" />
      <Button v-if="step === 'review'" type="button" label="Volver a ajustar" severity="secondary" outlined :disabled="saving" @click="back" />
      <Button v-if="step === 'adjust'" type="button" label="Siguiente: revisar cambios" icon="pi pi-arrow-right" :loading="previewing" :disabled="!canReview" @click="review" />
      <Button v-else type="button" label="Confirmar aprobación" icon="pi pi-check" :loading="saving" :disabled="!ready || previewing" @click="approve" />
      </div>
    </template>
  </Dialog>
</template>
