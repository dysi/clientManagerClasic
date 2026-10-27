<script setup lang="ts">
import { computed, ref, watch } from "vue";
import Button from "primevue/button";
import Card from "primevue/card";
import Checkbox from "primevue/checkbox";
import Dialog from "primevue/dialog";
import Message from "primevue/message";
import DateInput from "../../components/DateInput.vue";
import LicenseStatus from "../../components/LicenseStatus.vue";
import ListPaginator from "../../components/ListPaginator.vue";
import ListToolbar from "../../components/ListToolbar.vue";
import { usePagedList } from "../../composables/usePagedList";
import { useConfirmAction } from "../../composables/useConfirmAction";
import { useDraft } from "../../composables/useDraft";
import { displayDate } from "../../services/format";
import { approvePayment, previewApproval, type ReviewDetail, type LicenseOption, type LicenseAction, type ApprovalPreview } from "./review";
const props = defineProps<{ detail: ReviewDetail }>();
const emit = defineEmits<{ close: []; approved: [] }>();
const confirm = useConfirmAction();
interface Selection { license: LicenseOption; unlock: boolean; updateExpiry: boolean; lockDate: string | null; confirmShortening: boolean }
const draft = ref<{ selections: Selection[] }>({ selections: props.detail.associatedLicense ? [{ license: props.detail.associatedLicense, unlock: false, updateExpiry: false, lockDate: props.detail.associatedLicense.suggestedDate, confirmShortening: false }] : [] });
const { saving, error, save, dirty, reset } = useDraft(draft);
const preview = ref<ApprovalPreview | null>(null);
const previewing = ref(false);
const signature = computed(() => JSON.stringify(draft.value.selections.map(row => [row.license.id, row.license.revision, row.unlock, row.updateExpiry, row.updateExpiry ? row.lockDate : null])));
let sequence = 0;
watch(signature, () => { sequence++; preview.value = null; for (const row of draft.value.selections) row.confirmShortening = false; });
const { rows, keyword, loading, error: listError, page, size, total, search, paginate } = usePagedList<LicenseOption>(`/web/api/payments/${props.detail.payment.id}/licenses`);
function add(license: LicenseOption) {
  if (draft.value.selections.some(row => row.license.id === license.id)) return;
  if (draft.value.selections.length >= 100) { error.value = "Podés elegir hasta 100 licencias por aprobación."; return; }
  draft.value.selections.push({ license: { ...license }, unlock: false, updateExpiry: false, lockDate: license.suggestedDate, confirmShortening: false });
}
function actions(): LicenseAction[] { return draft.value.selections.filter(row => row.unlock || row.updateExpiry).map(row => ({ licenseId: row.license.id, revision: row.license.revision, unlock: row.unlock, updateExpiry: row.updateExpiry, lockDate: row.updateExpiry ? row.lockDate : null, confirmShortening: row.confirmShortening })); }
async function review() {
  if (previewing.value || saving.value) return;
  const current = ++sequence; previewing.value = true; error.value = ""; preview.value = null;
  try { const result = await previewApproval(props.detail.payment.id, props.detail.version, actions()); if (current === sequence) preview.value = result; }
  catch (failure) { if (current === sequence) error.value = failure instanceof Error ? failure.message : "No se pudo preparar la revisión."; }
  finally { previewing.value = false; }
}
const ready = computed(() => preview.value != null && preview.value.licenses.every(item => !item.shortening || draft.value.selections.find(row => row.license.id === item.id)?.confirmShortening));
async function approve() {
  const reviewed = preview.value;
  if (!reviewed || !ready.value) return;
  const result = await save(() => approvePayment(props.detail.payment.id, props.detail.version, actions(), reviewed), "El pago quedó aprobado.");
  if (result) { reset(); emit("approved"); }
}
async function close() { if (!saving.value && (!dirty.value || await confirm("¿Descartar las acciones elegidas para esta aprobación?", "Cerrar aprobación"))) { reset(); emit("close"); } }
</script>
<template>
  <Dialog :visible="true" modal header="Aprobar pago" class="w-[min(72rem,calc(100vw-2rem))]" :closable="!saving" :close-on-escape="!saving" @update:visible="!$event && close()">
    <div class="flex flex-col gap-5" :aria-busy="saving || previewing">
      <Message v-if="error" severity="error" role="alert">{{ error }}</Message>
      <Message severity="info" :closable="false">Podés aprobar sin modificar licencias. Las acciones son opcionales y no cambian el saldo informado del cliente.</Message>
      <p v-if="detail.associatedLicense" class="text-sm">Se propone la licencia asociada al pago; ninguna acción se selecciona automáticamente.</p>
      <Card v-for="row in draft.selections" :key="row.license.id"><template #title>{{ row.license.companyName }} (#{{ row.license.id }})</template><template #content>
        <div class="mb-4 flex flex-wrap items-center gap-3"><LicenseStatus :status="row.license.status" /><span>Vencimiento actual: {{ displayDate(row.license.lockDate) }}</span></div>
        <div class="grid gap-4 sm:grid-cols-2">
          <div class="flex items-center gap-2"><Checkbox :input-id="'unlock-' + row.license.id" v-model="row.unlock" binary :disabled="saving || previewing" /><label :for="'unlock-' + row.license.id">Quitar bloqueo forzado</label></div>
          <div class="flex items-center gap-2"><Checkbox :input-id="'expiry-' + row.license.id" v-model="row.updateExpiry" binary :disabled="saving || previewing" /><label :for="'expiry-' + row.license.id">Actualizar vencimiento</label></div>
          <div v-if="row.updateExpiry" class="flex flex-col gap-2"><label :for="'date-' + row.license.id">Vencimiento propuesto</label><DateInput :input-id="'date-' + row.license.id" v-model="row.lockDate" :disabled="saving || previewing" /></div>
        </div>
        <Button type="button" label="Quitar de la selección" text severity="secondary" :disabled="saving || previewing" @click="draft.selections = draft.selections.filter(item => item.license.id !== row.license.id)" />
      </template></Card>
      <details><summary class="cursor-pointer font-medium text-primary">Agregar otras licencias del cliente</summary><div class="mt-4">
        <Message v-if="listError" severity="error">{{ listError }}</Message>
        <ListToolbar v-model="keyword" :loading="loading" placeholder="Buscar licencia del cliente" @search="search" />
        <ul class="flex flex-col gap-3"><li v-for="license in rows" :key="license.id" class="flex min-w-0 flex-wrap items-center justify-between gap-2 border-b border-surface py-2"><span class="break-words">{{ license.companyName }} (#{{ license.id }}) · {{ displayDate(license.lockDate) }}</span><Button type="button" label="Agregar licencia" size="small" outlined :disabled="saving || previewing || draft.selections.some(row => row.license.id === license.id)" @click="add(license)" /></li></ul>
        <ListPaginator :page="page" :size="size" :total="total" @change="paginate" />
      </div></details>
      <Button type="button" label="Revisar aprobación" icon="pi pi-eye" outlined :loading="previewing" :disabled="saving" @click="review" />
      <section v-if="preview" class="flex flex-col gap-4" aria-label="Vista previa de aprobación">
        <h3 class="text-lg font-semibold">Fecha actual: {{ displayDate(preview.today) }}</h3>
        <Message v-if="!preview.licenses.length" severity="info" :closable="false">Se aprobará el pago sin modificar licencias.</Message>
        <Card v-for="item in preview.licenses" :key="item.id"><template #title>{{ item.companyName }}</template><template #content>
          <dl class="grid gap-3 sm:grid-cols-2"><div><dt>Vencimiento anterior</dt><dd>{{ displayDate(item.lockDate) }}</dd></div><div><dt>Vencimiento propuesto</dt><dd>{{ displayDate(item.proposedDate) }}</dd></div><div><dt>Días calendario desde hoy</dt><dd>{{ item.daysFromToday ?? 'Sin fecha' }}</dd></div><div><dt>Diferencia con el vencimiento anterior</dt><dd>{{ item.differenceFromPrevious == null ? 'Sin fecha anterior' : item.differenceFromPrevious + ' días' }}</dd></div></dl>
          <p v-if="item.unlock" class="mt-3">Se quitará el bloqueo forzado si estaba activo.</p>
          <div class="mt-3 flex flex-wrap items-center gap-2"><span>Estado resultante:</span><LicenseStatus :status="item.resultingStatus" /></div>
          <Message v-if="item.resultingStatus === 'EXPIRED' || item.resultingStatus === 'BLOCKED' || item.resultingStatus === 'UNKNOWN'" severity="warn" :closable="false" class="mt-3">Esta acción no deja la licencia vigente: revisá las condiciones indicadas.</Message>
          <template v-if="item.shortening"><Message severity="warn" :closable="false" class="mt-3">El vencimiento se acortará.</Message><div class="mt-3 flex items-center gap-2"><Checkbox :input-id="'shorten-' + item.id" :model-value="draft.selections.find(row => row.license.id === item.id)?.confirmShortening ?? false" binary :disabled="saving" @update:model-value="draft.selections = draft.selections.map(row => row.license.id === item.id ? { ...row, confirmShortening: $event === true } : row)" /><label :for="'shorten-' + item.id">Confirmo que quiero acortar este vencimiento</label></div></template>
        </template></Card>
      </section>
    </div>
    <template #footer><Button type="button" label="Cancelar" severity="secondary" text :disabled="saving" @click="close" /><Button type="button" label="Confirmar aprobación" icon="pi pi-check" :loading="saving" :disabled="!ready || previewing" @click="approve" /></template>
  </Dialog>
</template>
