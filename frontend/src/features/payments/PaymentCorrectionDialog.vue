<script setup lang="ts">
import { onMounted, ref } from "vue";
import Dialog from "primevue/dialog";
import Button from "primevue/button";
import InputNumber from "primevue/inputnumber";
import InputText from "primevue/inputtext";
import Textarea from "primevue/textarea";
import AutoComplete from "primevue/autocomplete";
import Message from "primevue/message";
import PaymentSyncWarning from "./PaymentSyncWarning.vue";
import Select from "primevue/select";
import FormField from "../../components/FormField.vue";
import DateInput from "../../components/DateInput.vue";
import { useDraft } from "../../composables/useDraft";
import { request } from "../../services/http";
import type { ReviewDetail, LicenseOption } from "./review";
interface BankDestination { id: number; bank: string; holder: string; cbu: string; alias: string | null; revision: string }
const props = defineProps<{ detail: ReviewDetail }>();
const emit = defineEmits<{ close: []; saved: [detail: ReviewDetail] }>();
const payment = props.detail.payment;
const form = ref<{ amount: number | null; issuedDate: string | null; issuedTime: string; contactPhone: string; licenseId: number | null; reason: string; destination: BankDestination | null }>({ amount: payment.amount, issuedDate: payment.issuedAt.split("T")[0] ?? null,
  issuedTime: payment.issuedAt.split("T")[1] ?? "", contactPhone: props.detail.contactPhone,
  licenseId: payment.licenseId, reason: "", destination: null });
const accounts = ref<BankDestination[]>([]), accountsLoading = ref(false), accountsError = ref("");
async function loadAccounts() {
  accountsLoading.value = true; accountsError.value = "";
  try { accounts.value = await request<BankDestination[]>(`/web/api/payments/${payment.id}/bank-accounts`); }
  catch (failure) { accountsError.value = failure instanceof Error ? failure.message : "No se pudieron cargar las cuentas."; }
  finally { accountsLoading.value = false; }
}
onMounted(loadAccounts);
const selectedLicense = ref<LicenseOption | string | null>(props.detail.associatedLicense);
const suggestions = ref<LicenseOption[]>([]), searchError = ref("");
let sequence = 0;
const { saving, error, fields, dirty, save, discard } = useDraft(form);
async function search(event: { query: string }) {
  const current = ++sequence;
  searchError.value = "";
  try {
    const params = new URLSearchParams({ keyword: event.query, size: "25" });
    const result = await request<{ content: LicenseOption[] }>(`/web/api/payments/${payment.id}/licenses?${params}`);
    if (current === sequence) suggestions.value = result.content;
  } catch {
    if (current === sequence) searchError.value = "No se pudieron cargar las licencias. Volvé a buscar.";
  }
}
function selectLicense(value: LicenseOption | string | null) {
  selectedLicense.value = value;
  form.value.licenseId = typeof value === "object" && value ? value.id : null;
}
async function close() { if (!saving.value && await discard()) emit("close"); }
async function submit() {
  if (!form.value.issuedDate || !form.value.issuedTime || form.value.amount == null) return;
  const result = await save(() => request<ReviewDetail>(`/web/api/payments/${payment.id}/correct`, {
    method: "POST", body: JSON.stringify({ version: props.detail.version, amount: form.value.amount,
      issuedAt: `${form.value.issuedDate}T${form.value.issuedTime}`, contactPhone: form.value.contactPhone.trim(),
      licenseId: form.value.licenseId, reason: form.value.reason.trim(),
      bankAccountId: form.value.destination?.id ?? null, accountRevision: form.value.destination?.revision ?? null })
  }), "Se corrigieron los datos del pago.");
  if (result) emit("saved", result);
}
</script>
<template>
  <Dialog :visible="true" modal header="Editar pago" class="w-[min(42rem,calc(100vw-2rem))]" :closable="!saving" :close-on-escape="!saving" @update:visible="!$event && close()">
    <form id="correctPayment" class="flex flex-col gap-4" :aria-busy="saving" @submit.prevent="submit">
      <Message severity="info" :closable="false">La corrección queda registrada con tu usuario y el motivo. Guardar conserva el estado actual del pago.</Message>
      <PaymentSyncWarning :synchronized-at="detail.payment.angryboxSynchronizedAt" />
      <Message v-if="error" severity="error" role="alert">{{ error }}</Message>
      <FormField id="correctionAmount" label="Importe en ARS" required :error="fields.amount" v-slot="field">
        <InputNumber input-id="correctionAmount" v-model="form.amount" mode="currency" currency="ARS" locale="es-AR" :min="0.01" :max="999999999999.99" :min-fraction-digits="2" :max-fraction-digits="2" :disabled="saving" :invalid="field.invalid" :input-props="{ 'aria-describedby': field.describedby }" fluid />
      </FormField>
      <div class="grid gap-4 sm:grid-cols-2">
        <FormField id="correctionDate" label="Fecha de emisión" required :error="fields.issuedAt" v-slot="field"><DateInput input-id="correctionDate" v-model="form.issuedDate" required :disabled="saving" :invalid="field.invalid" :describedby="field.describedby" /></FormField>
        <FormField id="correctionTime" label="Hora de emisión" required v-slot="field"><InputText id="correctionTime" v-model="form.issuedTime" type="time" step="any" required :disabled="saving" :aria-describedby="field.describedby" fluid /></FormField>
      </div>
      <FormField id="correctionPhone" label="Teléfono de contacto" required :error="fields.contactPhone" v-slot="field"><InputText id="correctionPhone" v-model="form.contactPhone" type="tel" maxlength="50" required :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid /></FormField>
      <FormField id="correctionLicense" label="Licencia asociada (opcional)" :error="fields.licenseId" v-slot="field">
        <AutoComplete input-id="correctionLicense" :model-value="selectedLicense" :suggestions="suggestions" option-label="companyName" dropdown force-selection :pt="{ dropdown: { 'aria-label': 'Mostrar licencias del cliente' } }" :disabled="saving" :invalid="field.invalid" :input-props="{ 'aria-describedby': field.describedby }" fluid @complete="search" @update:model-value="selectLicense" />
        <Button v-if="form.licenseId != null" type="button" label="Quitar licencia" text size="small" :disabled="saving" @click="selectLicense(null)" />
        <Message v-if="searchError" severity="error" size="small">{{ searchError }}</Message>
      </FormField>
      <FormField id="correctionDestination" label="Cambiar destino bancario" hint="Si no elegís otra cuenta, se conserva el destino registrado." :error="fields.bankAccountId || fields.accountRevision" v-slot="field">
        <p class="text-sm break-words">Destino registrado: {{ detail.destinationBank }} · {{ detail.destinationHolder }} · CBU {{ detail.destinationCbu }} · {{ detail.destinationAlias || 'Sin alias' }}</p>
        <Select input-id="correctionDestination" v-model="form.destination" :options="accounts" :option-label="(account: BankDestination) => `${account.bank} · ${account.holder} · ${account.cbu}`" placeholder="Conservar destino registrado" show-clear :loading="accountsLoading" :disabled="saving || accountsLoading" :invalid="field.invalid" :aria-describedby="field.describedby" fluid>
          <template #option="{ option }"><span class="whitespace-normal break-words">{{ option.bank }} · {{ option.holder }}<br>CBU {{ option.cbu }} · {{ option.alias || 'Sin alias' }}</span></template>
        </Select>
        <p v-if="form.destination" class="text-sm break-words">Nuevo destino: {{ form.destination.bank }} · {{ form.destination.holder }} · CBU {{ form.destination.cbu }} · {{ form.destination.alias || 'Sin alias' }}</p>
        <Message v-if="accountsError" severity="error" role="alert">{{ accountsError }}</Message>
        <p v-else-if="!accountsLoading && !accounts.length" class="text-sm text-muted-color">No hay cuentas activas disponibles.</p>
        <Button type="button" label="Actualizar cuentas" text size="small" :disabled="saving || accountsLoading" @click="loadAccounts" />
      </FormField>
      <FormField id="correctionReason" label="Motivo de la corrección (privado)" required :error="fields.reason" v-slot="field"><Textarea id="correctionReason" v-model="form.reason" rows="3" maxlength="1000" required :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid /></FormField>
    </form>
    <template #footer><Button type="button" label="Cancelar" text severity="secondary" :disabled="saving" @click="close" /><Button type="submit" form="correctPayment" label="Guardar corrección" :loading="saving" :disabled="!dirty || !form.reason.trim() || !form.contactPhone.trim() || form.amount == null" /></template>
  </Dialog>
</template>
