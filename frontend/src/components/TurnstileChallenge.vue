<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";
import Button from "primevue/button";
import Message from "primevue/message";
import { request } from "../services/http";
import { loadTurnstile, type TurnstileApi } from "../services/turnstile";

const props = defineProps<{ action: string }>();
const emit = defineEmits<{ ready: [value: boolean]; token: [value: string] }>();
const container = ref<HTMLElement | null>(null);
const error = ref("");
const loading = ref(false);
let api: TurnstileApi | undefined;
let widget: string | undefined;
let disposed = false;

function clear() {
  emit("ready", false);
  emit("token", "");
  if (api && widget !== undefined) api.remove(widget);
  widget = undefined;
}
async function start() {
  if (loading.value || disposed) return;
  clear();
  error.value = "";
  loading.value = true;
  try {
    const config = await request<{ enabled: boolean; siteKey: string }>("/web/session/turnstile");
    if (disposed) return;
    if (!config.enabled) { emit("ready", true); return; }
    if (!config.siteKey) throw new Error("La verificación de seguridad no está disponible. Volvé a intentar más tarde.");
    api = await loadTurnstile();
    if (disposed || !container.value) return;
    widget = api.render(container.value, {
      sitekey: config.siteKey,
      action: props.action,
      language: "es",
      size: "flexible",
      callback: token => { emit("token", token); emit("ready", true); error.value = ""; },
      "expired-callback": () => { emit("token", ""); emit("ready", false); },
      "error-callback": () => {
        emit("token", ""); emit("ready", false);
        error.value = "No se pudo completar la verificación. Volvé a intentar.";
      },
    });
  } catch (failure) {
    if (!disposed) error.value = failure instanceof Error ? failure.message : "No se pudo verificar el acceso.";
  } finally {
    loading.value = false;
  }
}
onMounted(start);
onBeforeUnmount(() => { disposed = true; clear(); });
</script>
<template>
  <div class="min-w-0" aria-label="Verificación de seguridad" :aria-busy="loading">
    <p v-if="loading" class="text-sm text-muted-color" role="status">Preparando verificación de seguridad…</p>
    <div ref="container" />
    <Message v-if="error" severity="error" role="alert" class="mt-2">{{ error }}</Message>
    <Button v-if="error" type="button" label="Reintentar verificación" text :disabled="loading" @click="start" />
  </div>
</template>
