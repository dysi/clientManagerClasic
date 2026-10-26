<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Menubar from "primevue/menubar";
import Button from "primevue/button";
import Message from "primevue/message";
import ConfirmDialog from "primevue/confirmdialog";
import Toast from "primevue/toast";
import { session, signOut } from "./features/auth/session";
import { useConfirmAction } from "./composables/useConfirmAction";
import { loginLocation } from "./features/auth/navigation";
const route = useRoute();
const router = useRouter();
const confirm = useConfirmAction();
const busy = ref(false);
const error = ref("");
const admin = computed(() => session.user?.roles.includes("ROLE_ADMIN"));
const navigation = [
  { label: "Clientes", icon: "pi pi-address-book", route: "/clients" },
  { label: "Licencias", icon: "pi pi-key", route: "/licenses" },
  { label: "Pagos", icon: "pi pi-wallet", route: "/payments" },
  { label: "Usuarios", icon: "pi pi-users", route: "/users" },
];
async function logout() {
  if (
    !(await confirm(
      "¿Cerrar la sesión? Los cambios sin guardar se perderán.",
      "Cerrar sesión",
    ))
  )
    return;
  busy.value = true;
  try {
    await signOut();
    await router.replace("/login");
  } catch (failure) {
    error.value =
      failure instanceof Error
        ? failure.message
        : "No se pudo cerrar la sesión.";
  } finally {
    busy.value = false;
  }
}
function expired() {
  session.user = null;
  if (route.path !== "/login")
    void router.replace(loginLocation(route.fullPath));
}
function focusContent() {
  document.getElementById("main")?.focus();
}
onMounted(() => window.addEventListener("session-expired", expired));
onUnmounted(() => window.removeEventListener("session-expired", expired));
</script>
<template>
  <ConfirmDialog /><Toast />
  <RouterView v-if="route.path === '/login'" />
  <div v-else class="min-h-screen bg-surface-50 text-color">
    <a
      href="#main"
      @click.prevent="focusContent"
      class="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:rounded-border focus:bg-surface-0 focus:p-3"
      >Saltar al contenido</a
    >
    <header class="border-b border-surface bg-surface-0">
      <Menubar
        :model="admin ? navigation : []"
        class="mx-auto max-w-screen-2xl rounded-none border-0 px-4 py-3 sm:px-8"
      >
        <template #start
          ><RouterLink
            to="/clients"
            class="mr-6 flex items-center gap-3 font-semibold text-primary"
            ><i class="pi pi-building text-xl" aria-hidden="true" /><span
              >DySI
              <span class="hidden font-normal text-muted-color sm:inline"
                >Client Manager</span
              ></span
            ></RouterLink
          ></template
        >
        <template #item="{ item, props }"
          ><RouterLink
            :to="item.route"
            v-bind="props.action"
            :aria-current="
              route.path.startsWith(item.route) ? 'page' : undefined
            "
            ><i :class="item.icon" aria-hidden="true" /><span>{{
              item.label
            }}</span></RouterLink
          ></template
        >
        <template #end
          ><div class="flex items-center gap-2">
            <span class="hidden text-sm text-muted-color md:inline">{{
              session.user?.username
            }}</span
            ><Button
              label="Cerrar sesión"
              icon="pi pi-sign-out"
              text
              severity="secondary"
              :loading="busy"
              @click="logout"
            /></div
        ></template>
      </Menubar>
    </header>
    <main id="main" tabindex="-1" class="mx-auto max-w-screen-2xl p-4 sm:p-8">
      <Message v-if="error" severity="error" class="mb-4">{{ error }}</Message>
      <RouterView v-if="admin" :key="route.fullPath" />
      <Message v-else severity="warn" :closable="false"
        >Tu cuenta no tiene permisos de administración. Contactá al
        administrador si necesitás acceso.</Message
      >
    </main>
    <footer
      class="mx-auto max-w-screen-2xl px-4 py-6 text-sm text-muted-color sm:px-8"
    >
      DySI · Administración de AngryBox
    </footer>
  </div>
</template>
