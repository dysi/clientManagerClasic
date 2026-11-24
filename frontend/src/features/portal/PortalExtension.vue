<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import Button from "primevue/button";
import Dialog from "primevue/dialog";
import Message from "primevue/message";
import { displayDate } from "../../services/format";
import { requestLicenseExtension, type PortalExtension } from "../../services/portal";

const props = defineProps<{ extension: PortalExtension }>();
const emit = defineEmits<{ updated: [extension: PortalExtension]; refresh: [] }>();
const dialog = ref(false);
const saving = ref(false);
const error = ref("");
const duration = computed(() => new Intl.NumberFormat("es-AR", { maximumFractionDigits: 4 }).format(props.extension.durationDays));
let timer: ReturnType<typeof setInterval> | undefined;
function refresh() { if (!saving.value) emit("refresh"); }
onMounted(() => {
  timer = setInterval(refresh, 60_000);
  window.addEventListener("focus", refresh);
});
onUnmounted(() => {
  clearInterval(timer);
  window.removeEventListener("focus", refresh);
});
async function submit() {
  if (saving.value) return;
  saving.value = true;
  error.value = "";
  try {
    emit("updated", await requestLicenseExtension());
    dialog.value = false;
  } catch (failure) {
    error.value = failure instanceof Error ? failure.message : "No se pudo solicitar la extensión. Volvé a intentar.";
    emit("refresh");
  } finally { saving.value = false; }
}
</script>

<template>
  <Message v-if="extension.active" severity="warn" :closable="false" role="status" class="mb-6">
    <div class="flex flex-col gap-3">
      <h2 class="text-xl font-bold">Extensión temporal vigente hasta el {{ displayDate(extension.expiresOn) }}</h2>
      <p>Podés seguir usando las licencias vencidas que no tengan un bloqueo administrativo. La extensión no modifica su vencimiento original.</p>
      <p v-if="extension.origin === 'CLIENT_REQUEST'">Regularizá tu deuda antes de que termine la extensión y registrá el pago desde <strong>Informar un nuevo pago</strong>.</p>
      <p v-else>Se otorgó esta extensión al informar tu pago. Consultá su estado en <strong>Mis pagos informados</strong>. Si ese pago es rechazado, la extensión quedará sin efecto.</p>
      <p class="font-semibold">Al terminar este plazo, las licencias que sigan vencidas volverán a bloquearse. Para solicitar otra extensión, necesitás que el último pago informado después de esta extensión esté aprobado.</p>
    </div>
  </Message>
  <Message v-else-if="extension.expiredLicenses || extension.reason === 'FORCED_LOCK'" severity="error" :closable="false" role="alert" class="mb-6">
    <div class="flex flex-col items-start gap-3">
      <h2 class="text-xl font-bold">{{ extension.reason === 'FORCED_LOCK' ? 'Licencias con bloqueo administrativo' : 'Licencia del sistema vencida' }}</h2>
      <template v-if="extension.eligible">
        <p>Podés solicitar una extensión temporal para seguir trabajando mientras regularizás tu deuda.</p>
        <Button type="button" label="Solicitar extensión temporal" icon="pi pi-clock" @click="error = ''; dialog = true" />
      </template>
      <p v-else>{{ extension.message }}</p>
    </div>
  </Message>
  <Dialog v-model:visible="dialog" modal header="Solicitar extensión temporal" class="w-full max-w-lg" :closable="!saving" :close-on-escape="!saving" :draggable="false">
    <p>Podés solicitar una extensión temporal de {{ duration }} {{ extension.durationDays === 1 ? 'día' : 'días' }} para seguir usando el sistema mientras regularizás tu deuda.</p>
    <p class="mt-3">La extensión no modifica el vencimiento original de tus licencias ni habilita las que tengan un bloqueo administrativo.</p>
    <Message v-if="error" severity="error" role="alert" class="mt-4">{{ error }}</Message>
    <template #footer>
      <div class="flex flex-wrap justify-end gap-3">
        <Button type="button" label="Cancelar" severity="secondary" outlined :disabled="saving" @click="dialog = false" />
        <Button type="button" label="Solicitar extensión" :loading="saving" :disabled="saving || !extension.eligible" @click="submit" />
      </div>
    </template>
  </Dialog>
</template>
