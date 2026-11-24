<script setup lang="ts">
import { computed, ref } from "vue";
import Dialog from "primevue/dialog";
import Button from "primevue/button";
import RadioButton from "primevue/radiobutton";
import Message from "primevue/message";
import DateInput from "../../components/DateInput.vue";
import FormField from "../../components/FormField.vue";
import { useDraft } from "../../composables/useDraft";
import { displayDate, today } from "../../services/format";
import { updateSelectedLicenses, type License, type LicenseBatchAction } from "../../services/admin";

const props = defineProps<{ licenses: License[] }>();
const emit = defineEmits<{ close: []; saved: [licenses: License[]] }>();
const step = ref(1);
const form = ref<{ action: LicenseBatchAction | null; lockDate: string | null }>({ action: null, lockDate: today() });
const { saving, error, save, discard } = useDraft(form);
const canAdvance = computed(() => form.value.action != null && (step.value !== 2 || form.value.action !== "LOCK_DATE" || !!form.value.lockDate));
const shortened = computed(() => {
  const date = form.value.lockDate;
  return form.value.action === "LOCK_DATE" && date
    ? props.licenses.filter(license => license.lockDate && license.lockDate > date) : [];
});
const titles = ["Elegir acción", "Configurar cambio", "Revisar y confirmar"];
async function close() {
  if (!saving.value && await discard()) emit("close");
}
async function submit() {
  if (saving.value || !canAdvance.value) return;
  if (step.value < 3) { step.value++; return; }
  const action = form.value.action;
  if (!action || (action === "LOCK_DATE" && !form.value.lockDate)) return;
  const result = await save(() => updateSelectedLicenses(props.licenses.map(license => license.id), action, form.value.lockDate),
    `Se actualizaron ${props.licenses.length} licencias.`);
  if (result) emit("saved", result);
}
</script>
<template>
  <Dialog :visible="true" modal header="Modificar seleccionados" class="w-[min(48rem,calc(100vw-2rem))]"
    :draggable="false" :closable="!saving" :close-on-escape="!saving" @update:visible="!$event && close()">
    <form id="licenseBatchForm" class="flex min-w-0 flex-col gap-4" :aria-busy="saving" @submit.prevent="submit">
      <p class="text-sm text-muted-color" aria-live="polite">Paso {{ step }} de 3 · {{ titles[step - 1] }}</p>
      <p><strong>{{ licenses.length }} {{ licenses.length === 1 ? 'licencia seleccionada' : 'licencias seleccionadas' }}</strong> de la página visible.</p>
      <Message v-if="error" severity="error" role="alert" :closable="false">{{ error }}</Message>
      <fieldset v-if="step === 1" class="flex flex-col gap-4">
        <legend class="mb-4 font-semibold">¿Qué querés hacer?</legend>
        <div class="flex items-center gap-2">
          <RadioButton v-model="form.action" input-id="batch-date" name="batchAction" value="LOCK_DATE" />
          <label for="batch-date">Modificar fecha de vencimiento</label>
        </div>
        <div class="flex items-center gap-2">
          <RadioButton v-model="form.action" input-id="batch-block" name="batchAction" value="BLOCK" />
          <label for="batch-block">Bloquear</label>
        </div>
        <div class="flex items-center gap-2">
          <RadioButton v-model="form.action" input-id="batch-unblock" name="batchAction" value="UNBLOCK" />
          <label for="batch-unblock">Desbloquear</label>
        </div>
      </fieldset>
      <template v-else-if="step === 2">
        <template v-if="form.action === 'LOCK_DATE'">
          <Message severity="warn" :closable="false">La fecha ingresada se aplicará a todas las licencias seleccionadas. Reemplazará sus fechas actuales y conservará los bloqueos forzados.</Message>
          <FormField id="batch-lock-date" label="Nueva fecha de vencimiento" required
            hint="Corresponde a la fecha de bloqueo: la licencia vence al comenzar ese día." v-slot="field">
            <DateInput v-model="form.lockDate" input-id="batch-lock-date" required :describedby="field.describedby" />
          </FormField>
        </template>
        <Message v-else-if="form.action === 'BLOCK'" severity="warn" :closable="false">Se aplicará bloqueo forzado a todas las licencias seleccionadas. Las que ya están bloqueadas seguirán bloqueadas. Sus fechas no cambiarán.</Message>
        <Message v-else severity="warn" :closable="false">Se quitará el bloqueo forzado de todas las licencias seleccionadas. Sus fechas no cambiarán: si están vencidas o no tienen fecha, no quedarán vigentes. La baja del cliente también seguirá impidiendo el acceso.</Message>
      </template>
      <template v-else>
        <h3 class="font-semibold">Revisá los cambios antes de confirmar</h3>
        <p v-if="form.action === 'LOCK_DATE'">Nueva fecha de vencimiento para todas: <strong>{{ displayDate(form.lockDate) }}</strong>.</p>
        <p v-else-if="form.action === 'BLOCK'">Se aplicará <strong>bloqueo forzado</strong> a todas las licencias de esta lista.</p>
        <Message v-else severity="warn" :closable="false">Se quitará el bloqueo forzado sin modificar los vencimientos ni la baja del cliente. Desbloquear no garantiza que la licencia quede vigente.</Message>
        <Message v-if="shortened.length" severity="warn" :closable="false">Vas a acortar el vencimiento de {{ shortened.length }} {{ shortened.length === 1 ? 'licencia' : 'licencias' }}.</Message>
        <Message v-if="form.action === 'LOCK_DATE' && form.lockDate && form.lockDate <= today()" severity="warn" :closable="false">La fecha elegida es hoy o anterior: las licencias quedarán vencidas. Las que tienen bloqueo forzado seguirán bloqueadas.</Message>
        <ul class="max-h-72 space-y-3 overflow-y-auto rounded-border border border-surface p-3">
          <li v-for="license in licenses" :key="license.id" class="min-w-0 text-sm [overflow-wrap:anywhere]">
            <p class="font-semibold">{{ license.companyName }} · Empresa #{{ license.companyId }}</p>
            <p class="text-muted-color">{{ license.clientName }}</p>
            <template v-if="form.action === 'LOCK_DATE'">
              <p>{{ displayDate(license.lockDate) }} → {{ displayDate(form.lockDate) }}</p>
              <p v-if="license.locked">Conserva el bloqueo forzado.</p>
            </template>
            <p v-else-if="form.action === 'BLOCK'">{{ license.locked ? 'Ya bloqueada: conserva el bloqueo.' : 'Se aplicará bloqueo forzado.' }}</p>
            <template v-else>
              <p>{{ license.locked ? 'Se quitará el bloqueo forzado.' : 'Ya está sin bloqueo forzado: se conserva.' }}</p>
              <p>Vencimiento sin cambios: {{ displayDate(license.lockDate) }}.</p>
              <p v-if="license.clientRemoved">El cliente está dado de baja: seguirá impidiendo el acceso.</p>
            </template>
          </li>
        </ul>
        <p class="text-sm text-muted-color">Solo se modifica el campo elegido. Si alguna licencia no puede modificarse, no se aplicará ningún cambio.</p>
      </template>
    </form>
    <template #footer>
      <div class="flex w-full flex-wrap justify-end gap-2">
        <Button type="button" label="Cancelar" severity="secondary" text :disabled="saving" @click="close" />
        <Button v-if="step > 1" type="button" label="Atrás" severity="secondary" outlined :disabled="saving" @click="step--" />
        <Button v-if="step < 3" type="submit" form="licenseBatchForm" :label="step === 1 ? 'Siguiente' : 'Revisar cambios'"
          icon="pi pi-arrow-right" :disabled="!canAdvance" />
        <Button v-else type="submit" form="licenseBatchForm" label="Confirmar cambios" icon="pi pi-check"
          :severity="form.action === 'BLOCK' ? 'danger' : 'primary'" :loading="saving" />
      </div>
    </template>
  </Dialog>
</template>
