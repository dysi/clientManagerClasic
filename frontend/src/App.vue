<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import Drawer from "primevue/drawer";
import Button from "primevue/button";
import Message from "primevue/message";
import ConfirmDialog from "primevue/confirmdialog";
import Toast from "primevue/toast";
import WhatsAppHelp from "./components/WhatsAppHelp.vue";
import AdminNavigation from "./components/AdminNavigation.vue";
import { session, signOut } from "./features/auth/session";
import { useConfirmAction } from "./composables/useConfirmAction";
import { sessionHome, loginLocation } from "./features/auth/navigation";
const route = useRoute();
const router = useRouter();
const confirm = useConfirmAction();
const busy = ref(false);
const error = ref("");
const helpVisible = computed(() => route.path === "/login" || route.path === "/access" || route.path === "/portal" || route.path.startsWith("/portal/"));
const admin = computed(() => session.user?.access === "ADMIN" && session.user.roles.includes("ROLE_ADMIN"));
const collapsed = ref(false);
const mobileNavigation = ref(false);
let openedFrom = "";
let desktopMedia: MediaQueryList | undefined;
function openNavigation() {
  openedFrom = route.fullPath;
  mobileNavigation.value = true;
}
function navigationClosed() {
  if (!admin.value) return;
  if (route.fullPath !== openedFrom || desktopMedia?.matches) focusContent();
  else document.getElementById("open-admin-navigation")?.focus();
}
function updateViewport() {
  if (desktopMedia?.matches) mobileNavigation.value = false;
}
function toggleSidebar() {
  collapsed.value = !collapsed.value;
  try { localStorage.setItem("dysi.adminNavigation.collapsed", String(collapsed.value)); }
  catch { /* La navegación sigue disponible si el navegador impide guardar preferencias. */ }
}
watch(() => route.fullPath, () => { mobileNavigation.value = false; });
watch(admin, () => { mobileNavigation.value = false; });
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
    const destination = admin.value ? "/login" : "/access";
    await signOut();
    await router.replace(destination);
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
  if (route.path !== "/login" && route.path !== "/access")
    void router.replace(loginLocation(route.fullPath));
}
function focusContent() {
  document.getElementById("main")?.focus();
}
onMounted(() => {
  window.addEventListener("session-expired", expired);
  try { collapsed.value = localStorage.getItem("dysi.adminNavigation.collapsed") === "true"; }
  catch { /* Se usa el panel expandido como valor inicial. */ }
  desktopMedia = window.matchMedia("(min-width: 1024px)");
  desktopMedia.addEventListener("change", updateViewport);
});
onUnmounted(() => {
  window.removeEventListener("session-expired", expired);
  desktopMedia?.removeEventListener("change", updateViewport);
});
</script>
<template>
  <ConfirmDialog /><Toast />
  <div :class="helpVisible ? 'pb-24' : undefined">
  <RouterView v-if="route.path === '/login' || route.path === '/access'" :key="route.path" />
  <div v-else class="min-h-screen bg-surface-50 text-color">
    <a
      href="#main"
      @click.prevent="focusContent"
      class="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:rounded-border focus:bg-surface-0 focus:p-3"
      >Saltar al contenido</a
    >
    <header class="border-b border-surface bg-surface-0">
      <div class="flex min-h-16 items-center justify-between gap-3 px-4 py-2 sm:px-8">
          <RouterLink
            :to="sessionHome()"
            class="flex min-w-0 items-center gap-3 font-semibold text-primary"
            ><i class="pi pi-building text-xl" aria-hidden="true" /><span
              >DySI
              <span :class="admin ? 'font-normal text-muted-color' : 'hidden font-normal text-muted-color sm:inline'"
                >{{ admin ? 'Client Manager' : 'Portal de clientes' }}</span
              ></span
            ></RouterLink
          >
          <div class="flex shrink-0 items-center gap-2">
            <span class="hidden text-sm text-muted-color md:inline">{{
              session.user?.username
            }}</span
            ><Button
              type="button"
              label="Cerrar sesión"
              aria-label="Cerrar sesión"
              icon="pi pi-sign-out"
              :pt="{ label: { class: 'hidden sm:inline' } }"
              text
              severity="secondary"
              :loading="busy"
              @click="logout"
            /></div
        >
      </div>
    </header>
    <div class="flex items-start">
    <aside v-if="admin" class="sticky top-0 hidden h-[calc(100dvh-4rem-1px)] shrink-0 overflow-y-auto border-r border-surface bg-surface-0 p-2 lg:block" :class="collapsed ? 'w-20' : 'w-64'" aria-label="Panel administrativo">
      <div class="mb-3 flex items-center" :class="collapsed ? 'justify-center' : 'justify-between px-2'">
        <span v-if="!collapsed" class="text-sm font-semibold text-muted-color">Administración</span>
        <Button type="button" :icon="collapsed ? 'pi pi-angle-double-right' : 'pi pi-angle-double-left'" :aria-label="collapsed ? 'Expandir navegación' : 'Colapsar navegación'" :title="collapsed ? 'Expandir navegación' : 'Colapsar navegación'" :aria-expanded="!collapsed" aria-controls="admin-navigation" text severity="secondary" @click="toggleSidebar" />
      </div>
      <AdminNavigation id="admin-navigation" :collapsed="collapsed" />
    </aside>
    <Drawer v-if="admin" v-model:visible="mobileNavigation" header="Administración" aria-label="Navegación administrativa" class="w-72 max-w-[90vw]" :block-scroll="true" :close-button-props="{ 'aria-label': 'Cerrar navegación' }" @after-hide="navigationClosed">
      <AdminNavigation id="admin-mobile-navigation" />
    </Drawer>
    <div class="min-w-0 flex-1" :class="admin ? 'flex min-h-[calc(100dvh-4rem-1px)] flex-col' : undefined">
    <div v-if="admin" class="px-4 pt-4 lg:hidden">
      <Button id="open-admin-navigation" type="button" label="Menú" icon="pi pi-bars" aria-label="Abrir navegación" aria-controls="admin-mobile-navigation" :aria-expanded="mobileNavigation" outlined severity="secondary" @click="openNavigation" />
    </div>
    <main id="main" tabindex="-1" class="mx-auto w-full max-w-screen-2xl p-4 sm:p-8" :class="admin ? 'flex-1' : undefined">
      <Message v-if="error" severity="error" class="mb-4">{{ error }}</Message>
      <RouterView v-if="admin || session.user?.access === 'PORTAL'" :key="route.fullPath" />
    </main>
    <footer
      class="mx-auto w-full max-w-screen-2xl px-4 py-6 text-sm text-muted-color sm:px-8"
    >
      DySI · {{ admin ? 'Administración de AngryBox' : 'Portal de clientes' }}
    </footer>
    </div>
    </div>
  </div>
  </div>
  <WhatsAppHelp v-if="helpVisible" />
</template>
