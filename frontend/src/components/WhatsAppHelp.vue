<script setup lang="ts">
import { onMounted, ref } from "vue";
import Button from "primevue/button";
import { request } from "../services/http";
const url = ref<string | null>(null);
const loading = ref(false);
const error = ref("");
async function load() {
  if (loading.value) return;
  loading.value = true; error.value = "";
  try {
    const contact = await request<{ whatsappUrl: string }>("/web/session/support");
    // Solo abrir el destino de WhatsApp previsto, aun ante una respuesta inesperada.
    if (!/^https:\/\/wa\.me\/[1-9][0-9]{6,14}$/.test(contact.whatsappUrl)) throw new Error("Contacto inválido");
    url.value = contact.whatsappUrl;
  } catch { error.value = "No se pudo cargar el contacto. Tocá para reintentar."; }
  finally { loading.value = false; }
}
onMounted(load);
</script>
<template>
  <div class="fixed right-4 bottom-[max(1rem,env(safe-area-inset-bottom))] z-40 sm:right-6">
    <Button :as="url ? 'a' : 'button'" :type="url ? undefined : 'button'" :href="url ?? undefined"
      :target="url ? '_blank' : undefined" :rel="url ? 'noopener noreferrer' : undefined"
      label="Necesito ayuda" icon="pi pi-whatsapp" severity="success" rounded class="shadow-md"
      aria-label="Necesito ayuda por WhatsApp (abre una nueva pestaña)" :title="error || 'Conversá con nosotros por WhatsApp'"
      :loading="loading" @click="!url && load()" />
  </div>
</template>
