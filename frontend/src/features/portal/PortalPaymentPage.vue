<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import Button from "primevue/button";
import Card from "primevue/card";
import Checkbox from "primevue/checkbox";
import ReceiptPicker from "./ReceiptPicker.vue";
import InputNumber from "primevue/inputnumber";
import InputText from "primevue/inputtext";
import Message from "primevue/message";
import RadioButton from "primevue/radiobutton";
import Skeleton from "primevue/skeleton";
import Textarea from "primevue/textarea";
import PageHeader from "../../components/PageHeader.vue";
import FormField from "../../components/FormField.vue";
import DateInput from "../../components/DateInput.vue";
import EntitySelect from "../../components/EntitySelect.vue";
import { useConfirmAction } from "../../composables/useConfirmAction";
import { useDraft } from "../../composables/useDraft";
import { request } from "../../services/http";
import { displayDateTime, money, today } from "../../services/format";
import { submitPayment, type PaymentFormContext, type PortalAccount, type PortalPayment, type ReceiptSelection } from "../../services/portal";

const router = useRouter();
const confirm = useConfirmAction();
const now = new Date();
interface PaymentDraft {
  bankAccountId: number | null; licenseId: number | null; amount: number | null; issuedDate: string | null;
  issuedTime: string; contactPhone: string; withoutReceipt: boolean; noReceiptReason: string; comment: string; receiptNames: string[]; receiptDescriptions: string[];
}
const form = ref<PaymentDraft>({ bankAccountId: null, licenseId: null,
  amount: null, issuedDate: today(),
  issuedTime: `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`,
  contactPhone: "", withoutReceipt: false, noReceiptReason: "", comment: "", receiptNames: [], receiptDescriptions: [] });
const { saving, error, fields, reset, save, discard } = useDraft(form);
const accounts = ref<PortalAccount[]>([]);
const loadingAccounts = ref(false);
const accountsError = ref("");
const total = computed(() => accounts.value.length);
const selected = ref<PortalAccount | null>(null);
const licenseLabel = ref("");
const receipts = ref<ReceiptSelection[]>([]);
const files = computed(() => receipts.value.map(item => item.file));
const limits = ref<PaymentFormContext | null>(null);
const loading = ref(true);
const loadError = ref("");
const step = ref(1);
const validation = ref("");
const copied = ref("");
const result = ref<PortalPayment | null>(null);
const submissionKey = ref(Array.from(crypto.getRandomValues(new Uint8Array(16)), (value) => value.toString(16).padStart(2, "0")).join(""));
const issuedAt = computed(() => `${form.value.issuedDate ?? ""}T${form.value.issuedTime}:00`);
const steps = ["Elegí la cuenta", "Completá el pago", "Revisá y confirmá"];

onMounted(async () => {
  try {
    limits.value = await request<PaymentFormContext>("/web/portal/payment-form");
    accounts.value = limits.value.accounts;
    form.value.contactPhone = limits.value.contactPhone ?? "";
    form.value.amount = limits.value.suggestedAmount;
    reset();
  } catch (failure) { loadError.value = failure instanceof Error ? failure.message : "No se pudo preparar la presentación."; }
  finally { loading.value = false; }
});
function selectAccount(account: PortalAccount) { selected.value = { ...account }; form.value.bankAccountId = account.id; }
async function refreshAccounts() {
  selected.value = null; form.value.bankAccountId = null; validation.value = ""; error.value = "";
  loadingAccounts.value = true; accountsError.value = "";
  try {
    const context = await request<PaymentFormContext>("/web/portal/payment-form");
    accounts.value = context.accounts; limits.value = context;
    // Actualizar cuentas no pisa el importe que el cliente haya escrito o limpiado.
  } catch (failure) { accountsError.value = failure instanceof Error ? failure.message : "No se pudieron actualizar las cuentas."; }
  finally { loadingAccounts.value = false; }
}
async function copy(value: string, label: string) {
  try { await navigator.clipboard.writeText(value); copied.value = `${label} copiado.`; }
  catch { copied.value = "No se pudo copiar automáticamente. Seleccioná y copiá el texto de la cuenta."; }
}
function updateReceipts(value: ReceiptSelection[]) {
  receipts.value = value;
  form.value.receiptNames = value.map(item => JSON.stringify([item.file.name, item.file.size, item.file.lastModified]));
  form.value.receiptDescriptions = value.map(item => item.description);
}
async function toggleWithoutReceipt(value: unknown) {
  if (value === true && files.value.length && !(await confirm("¿Quitar los comprobantes seleccionados y presentar el pago sin ellos?", "Presentar sin comprobante"))) return;
  form.value.withoutReceipt = value === true;
  if (form.value.withoutReceipt) updateReceipts([]);
}
function validDetails(): boolean {
  const value = form.value;
  if (value.amount == null || !Number.isFinite(value.amount) || value.amount < 0.01 || value.amount > 999999999999.99
      || !/^\d+(\.\d{1,2})?$/.test(String(value.amount))) validation.value = "Ingresá un importe positivo con hasta dos decimales.";
  else if (!value.issuedDate || !/^\d{4}-\d{2}-\d{2}$/.test(value.issuedDate) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(value.issuedTime)) validation.value = "Completá la fecha y la hora de emisión.";
  else if (!value.contactPhone.trim()) validation.value = "Ingresá un teléfono de contacto.";
  else if (value.withoutReceipt && !value.noReceiptReason.trim()) validation.value = "Completá el motivo por el que no vas a subir comprobante.";
  else if (!value.withoutReceipt && !files.value.length) validation.value = "Adjuntá un comprobante o indicá que no lo vas a subir.";
  else if (limits.value && (files.value.length > limits.value.maxReceipts || files.value.some((file) => file.size === 0 || file.size > (limits.value?.maxFileBytes ?? 0)))) validation.value = "Revisá la cantidad y el tamaño de los comprobantes.";
  else return true;
  return false;
}
async function advance() {
  validation.value = "";
  if (step.value === 1) {
    if (form.value.bankAccountId == null || selected.value == null) { validation.value = "Elegí expresamente una cuenta destinataria."; return; }
    step.value = 2; return;
  }
  if (!validDetails()) return;
  if (step.value === 2) { step.value = 3; return; }
  const accountId = form.value.bankAccountId, amount = form.value.amount;
  const account = selected.value;
  if (accountId == null || amount == null || account == null) return;
  const payment = await save(() => submitPayment({ bankAccountId: accountId, accountRevision: account.revision, licenseId: form.value.licenseId,
    amount, issuedAt: issuedAt.value, contactPhone: form.value.contactPhone.trim(), method: "TRANSFERENCIA",
    withoutReceipt: form.value.withoutReceipt, noReceiptReason: form.value.withoutReceipt ? form.value.noReceiptReason.trim() : null,
    comment: form.value.comment.trim() || null, submissionKey: submissionKey.value, descriptions: receipts.value.map(item => item.description.trim() || null) }, files.value), "El pago quedó presentado y pendiente de revisión.");
  if (payment) { result.value = payment; updateReceipts([]); reset(); }
}
async function cancel() { if (await discard()) await router.push("/portal"); }
</script>
<template>
  <PageHeader :title="result ? 'Pago informado' : 'Informar un pago'" description="Transferencia en pesos argentinos.">
    <Button v-if="!result" type="button" label="Volver al portal" icon="pi pi-arrow-left" text :disabled="saving" @click="cancel" />
  </PageHeader>
  <Skeleton v-if="loading" height="16rem" />
  <Message v-else-if="loadError" severity="error" role="alert">{{ loadError }}</Message>
  <Card v-else-if="result">
    <template #title>Recibimos tu presentación #{{ result.id }}</template>
    <template #content>
      <Message severity="success" :closable="false">Tu pago de {{ money(result.amount) }} está {{ result.status === 'PENDIENTE' ? 'pendiente de revisión' : result.status.toLowerCase() }}.</Message>
      <p class="my-5">La presentación no modifica automáticamente el saldo ni la vigencia de tus licencias.</p>
      <Button as="router-link" to="/portal" label="Volver al portal" icon="pi pi-check" />
    </template>
  </Card>
  <form v-else novalidate :aria-busy="saving" @submit.prevent="advance">
    <ol class="mb-6 grid gap-3 sm:grid-cols-3" aria-label="Pasos de la presentación">
      <li v-for="(label, index) in steps" :key="label" :aria-current="step === index + 1 ? 'step' : undefined"
        :class="['rounded-border border p-3 text-sm', step === index + 1 ? 'border-primary bg-highlight font-semibold' : 'border-surface text-muted-color']">{{ index + 1 }}. {{ label }}</li>
    </ol>
    <Message v-if="error || validation" severity="error" role="alert" class="mb-5">{{ validation || error }}</Message>
    <div v-if="step === 1" class="flex flex-col gap-4">
      <Message v-if="accountsError" severity="error" role="alert">{{ accountsError }}</Message>
      <Message v-else-if="!loadingAccounts && total === 0" severity="warn" :closable="false">No hay cuentas destinatarias disponibles. Contactá al administrador; por ahora no podés presentar la transferencia.</Message>
      <Skeleton v-if="loadingAccounts" height="10rem" />
      <fieldset v-else class="flex min-w-0 flex-col gap-4 border-0 p-0">
        <legend class="mb-4 font-medium">Elegí la cuenta a la que realizaste la transferencia</legend>
        <Card v-for="account in accounts" :key="account.id" :class="form.bankAccountId === account.id ? 'outline-2 outline-primary' : ''">
          <template #title><div class="flex items-center gap-3">
            <RadioButton :input-id="`account-${account.id}`" name="destination" :model-value="form.bankAccountId" :value="account.id" :disabled="saving" @update:model-value="selectAccount(account)" />
            <label :for="`account-${account.id}`" class="cursor-pointer">{{ account.bank }}</label>
          </div></template>
          <template #content><dl class="grid min-w-0 gap-3 md:grid-cols-3">
            <div><dt class="text-sm text-muted-color">Titular</dt><dd class="break-words">{{ account.holder }}</dd></div>
            <div><dt class="text-sm text-muted-color">CBU</dt><dd class="flex min-w-0 flex-wrap items-center gap-2"><span class="break-all font-mono">{{ account.cbu }}</span>
              <Button type="button" icon="pi pi-copy" :aria-label="`Copiar CBU de ${account.bank}`" text size="small" @click="copy(account.cbu, 'CBU')" /></dd></div>
            <div v-if="account.alias"><dt class="text-sm text-muted-color">Alias</dt><dd class="flex min-w-0 flex-wrap items-center gap-2"><span class="break-all">{{ account.alias }}</span>
              <Button type="button" icon="pi pi-copy" :aria-label="`Copiar alias de ${account.bank}`" text size="small" @click="copy(account.alias, 'Alias')" /></dd></div>
          </dl></template>
        </Card>
      </fieldset>
      <p role="status" aria-live="polite" class="text-sm">{{ copied }}</p>
      <div><Button type="button" label="Actualizar cuentas" outlined :disabled="loadingAccounts" @click="refreshAccounts" /></div>
    </div>
    <Card v-show="step === 2"><template #title>Datos de la transferencia</template><template #content>
      <div class="grid gap-5 md:grid-cols-2">
        <FormField id="amount" label="Importe en ARS" required :hint="limits?.suggestedAmount != null ? 'Precargamos la deuda informada. Podés modificar o limpiar el importe.' : 'Ingresá el importe de la transferencia.'" :error="fields.amount" v-slot="field">
          <InputNumber input-id="amount" v-model="form.amount" :allow-empty="true" mode="currency" currency="ARS" locale="es-AR" :min="0.01" :max="999999999999.99" :min-fraction-digits="2" :max-fraction-digits="2" :disabled="saving" :invalid="field.invalid" :input-props="{ 'aria-describedby': field.describedby }" fluid />
          <div><Button type="button" label="Limpiar importe" icon="pi pi-times" text size="small" :disabled="saving || form.amount == null" @click="form.amount = null" /></div>
        </FormField>
        <FormField id="contactPhone" label="Teléfono de contacto" required :error="fields.contactPhone" v-slot="field">
          <InputText id="contactPhone" v-model="form.contactPhone" type="tel" maxlength="50" :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
        </FormField>
        <FormField id="issuedDate" label="Fecha de emisión" required :error="fields.issuedAt" v-slot="field">
          <DateInput input-id="issuedDate" v-model="form.issuedDate" required :disabled="saving" :invalid="field.invalid" :describedby="field.describedby" />
        </FormField>
        <FormField id="issuedTime" label="Hora de emisión" required v-slot="field">
          <InputText id="issuedTime" v-model="form.issuedTime" type="time" :disabled="saving" :aria-describedby="field.describedby" fluid />
        </FormField>
        <FormField v-if="limits?.sensitiveAccess" id="license" label="Licencia (opcional)" hint="Podés informar el pago sin asociarlo a una licencia." :error="fields.licenseId" v-slot="field">
          <EntitySelect input-id="license" v-model="form.licenseId" :label="licenseLabel" kind="portal-licenses" :disabled="saving" :invalid="field.invalid" :describedby="field.describedby" @selected="licenseLabel = $event?.label ?? ''" />
          <Button v-if="form.licenseId != null" type="button" label="Quitar licencia" text size="small" @click="form.licenseId = null" />
        </FormField>
      </div>
      <div class="mt-6 flex flex-col gap-4">
        <div class="flex items-start gap-3"><Checkbox input-id="withoutReceipt" :model-value="form.withoutReceipt" binary :disabled="saving" @update:model-value="toggleWithoutReceipt" /><label for="withoutReceipt">No voy a subir comprobante</label></div>
        <template v-if="form.withoutReceipt">
          <Message severity="warn" :closable="false">Sin un comprobante asociado, la verificación del pago puede extenderse hasta 48 horas.</Message>
          <FormField id="noReceiptReason" label="Motivo de ausencia de comprobante" required :error="fields.noReceiptReason" v-slot="field">
            <Textarea id="noReceiptReason" v-model="form.noReceiptReason" maxlength="1000" rows="3" :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
          </FormField>
        </template>
        <div v-else>
          <p class="mb-3 font-medium">Comprobantes *</p>
          <ReceiptPicker :model-value="receipts" :max-receipts="limits?.maxReceipts ?? 5" :max-file-bytes="limits?.maxFileBytes ?? 10485760" :disabled="saving" @update:model-value="updateReceipts" />
        </div>
        <FormField id="comment" label="Comentario público (opcional)" hint="Este comentario es independiente del motivo de ausencia de comprobante." :error="fields.comment" v-slot="field">
          <Textarea id="comment" v-model="form.comment" maxlength="4000" rows="3" :disabled="saving" :invalid="field.invalid" :aria-describedby="field.describedby" fluid />
        </FormField>
      </div>
    </template></Card>
    <Card v-if="step === 3"><template #title>Revisá la presentación</template><template #content>
      <dl class="grid gap-5 sm:grid-cols-2">
        <div><dt class="text-sm text-muted-color">Importe</dt><dd class="text-2xl font-semibold">{{ money(form.amount ?? 0) }}</dd></div>
        <div><dt class="text-sm text-muted-color">Emisión</dt><dd>{{ displayDateTime(issuedAt) }}</dd></div>
        <div><dt class="text-sm text-muted-color">Cuenta destinataria</dt><dd>{{ selected?.bank }} · {{ selected?.holder }}<br /><span class="break-all">{{ selected?.cbu }}</span><br />{{ selected?.alias }}</dd></div>
        <div><dt class="text-sm text-muted-color">Teléfono</dt><dd>{{ form.contactPhone }}</dd></div>
        <div><dt class="text-sm text-muted-color">Licencia</dt><dd>{{ form.licenseId == null ? 'Sin licencia asociada' : licenseLabel || '#' + form.licenseId }}</dd></div>
        <div><dt class="text-sm text-muted-color">Comprobantes</dt><dd v-if="form.withoutReceipt" class="whitespace-pre-wrap break-words">Sin comprobante. Motivo: {{ form.noReceiptReason }}</dd><dd v-else><ul><li v-for="item in receipts" :key="`${item.file.name}:${item.file.size}:${item.file.lastModified}`" class="break-all">{{ item.file.name }}<p v-if="item.description" class="whitespace-pre-wrap break-words text-sm text-muted-color">{{ item.description }}</p></li></ul></dd></div>
        <div v-if="form.comment" class="sm:col-span-2"><dt class="text-sm text-muted-color">Comentario público</dt><dd class="whitespace-pre-wrap break-words">{{ form.comment }}</dd></div>
      </dl>
      <Message v-if="form.withoutReceipt" severity="warn" :closable="false" class="mt-5">Sin un comprobante asociado, la verificación del pago puede extenderse hasta 48 horas.</Message>
      <Message severity="info" :closable="false" class="mt-5">El pago quedará pendiente de revisión. Después de presentarlo podrás agregar comentarios y comprobantes desde su detalle. Los datos del pago y los adjuntos enviados se conservan.</Message>
    </template></Card>
    <div class="mt-6 flex flex-wrap justify-end gap-3">
      <Button v-if="step > 1" type="button" label="Atrás" severity="secondary" outlined :disabled="saving" @click="step--; validation = ''" />
      <Button type="button" label="Cancelar" severity="secondary" text :disabled="saving" @click="cancel" />
      <Button type="submit" :label="step === 3 ? 'Confirmar presentación' : 'Continuar'" :icon="step === 3 ? 'pi pi-check' : 'pi pi-arrow-right'" :loading="saving" :disabled="step === 1 && (form.bankAccountId == null || loadingAccounts || total === 0)" />
    </div>
  </form>
</template>
