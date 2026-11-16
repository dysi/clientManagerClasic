<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Message from "primevue/message";
import ProgressSpinner from "primevue/progressspinner";
import { session, signInFromAngrybox } from "./session";
import { sessionHome } from "./navigation";

const route = useRoute();
const router = useRouter();
const error = ref("");
const busy = ref(true);

onMounted(async () => {
  const ticket = route.query.ticket;
  // El fragmento no llega a los logs HTTP y se elimina del historial antes del canje.
  try {
    await router.replace("/portal-access");
    if (typeof ticket !== "string" || !/^[A-Za-z0-9_-]{43}$/.test(ticket)) {
      throw new Error("Volvé a abrir el portal desde Angrybox para obtener un acceso nuevo.");
    }
    await signInFromAngrybox(ticket);
    await router.replace("/portal");
  } catch (failure) {
    error.value = failure instanceof Error ? failure.message : "No se pudo abrir el portal.";
  } finally {
    busy.value = false;
  }
});
</script>

<template>
  <section class="mx-auto max-w-xl p-6" aria-labelledby="portal-access-title" :aria-busy="busy">
    <h1 id="portal-access-title" class="mb-4 text-2xl font-semibold">Portal de clientes</h1>
    <div v-if="busy" class="flex items-center gap-4" role="status">
      <ProgressSpinner class="size-10" aria-label="Abriendo el portal" />
      <p>Estamos preparando el acceso a tu cuenta.</p>
    </div>
    <Message v-if="error" severity="error" :closable="false">{{ error }}</Message>
    <p v-if="error" class="mt-4 text-muted-color">Podés cerrar esta pestaña y volver a Angrybox.</p>
    <RouterLink v-if="error && session.user" :to="sessionHome()" class="mt-4 inline-block text-primary underline">Ir a la sesión abierta</RouterLink>
  </section>
</template>
